package br.com.empresa.helpdesk.notificacoes;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import br.com.empresa.helpdesk.notificacoes.application.*;
import br.com.empresa.helpdesk.notificacoes.domain.NotificacaoOutbox;
import br.com.empresa.helpdesk.notificacoes.infra.NotificacaoOutboxRepository;
import br.com.empresa.helpdesk.sla.application.AlertaSlaJob;
import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class NotificacaoSlaTest {
  private final Instant agora = Instant.parse("2026-09-28T12:00:00Z");
  private final Clock clock = Clock.fixed(agora, ZoneOffset.UTC);
  private final UsuarioService usuarios = mock(UsuarioService.class);
  private final ChamadoService chamados = mock(ChamadoService.class);
  private final NotificacaoOutboxRepository repository = mock(NotificacaoOutboxRepository.class);
  private final List<NotificacaoOutbox> enviados = new ArrayList<>();

  private NotificacaoService service(String modo, String enderecos) {
    Set<String> chaves = new HashSet<>();
    when(repository.existsByDedupKey(anyString()))
        .thenAnswer(i -> chaves.contains(i.getArgument(0)));
    when(repository.save(any()))
        .thenAnswer(
            i -> {
              NotificacaoOutbox item = i.getArgument(0);
              enviados.add(item);
              if (item.getDedupKey() != null) chaves.add(item.getDedupKey());
              return item;
            });
    return new NotificacaoService(
        repository, usuarios, chamados, clock, new DestinatariosTi(usuarios, modo, enderecos));
  }

  private ChamadoService.ResumoNotificacao resumo(long id, Long responsavel) {
    return new ChamadoService.ResumoNotificacao(
        id,
        "CH-2026-00000" + id,
        "Conteúdo sensível",
        9L,
        responsavel,
        agora.plusSeconds(1800),
        agora.plusSeconds(900),
        null,
        StatusChamado.ABERTO);
  }

  @Test
  void jobAlertaDoisPrazosIncluiSemResponsavelESemDuplicar() {
    when(usuarios.emailsTiAtivos()).thenReturn(List.of("ti@example.com"));
    when(usuarios.buscarAtivoPorId(7L))
        .thenReturn(Optional.of(Usuario.novoFuncionario("TI", "agente@example.com")));
    var atribuido = resumo(1, 7L);
    var semResponsavel = resumo(2, null);
    when(chamados.vencendoSla(agora, agora.plusSeconds(3600)))
        .thenReturn(List.of(atribuido, semResponsavel));
    when(chamados.vencendoPrimeiraResposta(agora, agora.plusSeconds(3600)))
        .thenReturn(List.of(semResponsavel));
    var job = new AlertaSlaJob(chamados, service("usuarios", ""), clock);
    job.executar();
    job.executar();
    assertThat(enviados).hasSize(3);
    assertThat(enviados)
        .extracting(NotificacaoOutbox::getDestinatario)
        .containsExactly("agente@example.com", "ti@example.com", "ti@example.com");
    assertThat(enviados)
        .extracting(NotificacaoOutbox::getTipo)
        .contains("SLA_RESOLUCAO", "SLA_PRIMEIRA_RESPOSTA");
    assertThat(enviados)
        .allSatisfy(
            item -> {
              assertThat(item.getPayload()).doesNotContainValue("Conteúdo sensível");
              assertThat(item.getDedupKey()).doesNotContain("@example.com");
            });
  }

  @Test
  void listaRecebeCriacaoEComentarioSemResponsavelSemConsultarTodosUsuariosTi() {
    var servico = service("lista", "fila@example.com, fila@example.com, suporte@example.com");
    when(chamados.resumoParaNotificacao(2L)).thenReturn(resumo(2, null));
    servico.criado(new ChamadoCriadoEvent(2L));
    servico.comentario(new ComentarioCriadoEvent(2L, 9L, false));
    assertThat(enviados).hasSize(4);
    assertThat(enviados)
        .extracting(NotificacaoOutbox::getDestinatario)
        .containsOnly("fila@example.com", "suporte@example.com");
    verify(usuarios, never()).emailsTiAtivos();
    servico.comentario(new ComentarioCriadoEvent(2L, 9L, true));
    assertThat(enviados).hasSize(4);
  }

  @Test
  void padraoMantemUsuariosAtivosEConfiguracaoInvalidaFalha() {
    when(usuarios.emailsTiAtivos()).thenReturn(List.of("ti@example.com"));
    var servico = service("usuarios", "");
    when(chamados.resumoParaNotificacao(1L)).thenReturn(resumo(1, 7L));
    servico.criado(new ChamadoCriadoEvent(1L));
    assertThat(enviados)
        .extracting(NotificacaoOutbox::getDestinatario)
        .containsExactly("ti@example.com");
    assertThatThrownBy(() -> new DestinatariosTi(usuarios, "lista", ""))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new DestinatariosTi(usuarios, "outro", ""))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
