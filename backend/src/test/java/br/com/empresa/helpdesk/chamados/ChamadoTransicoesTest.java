package br.com.empresa.helpdesk.chamados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.empresa.helpdesk.chamados.domain.Chamado;
import br.com.empresa.helpdesk.chamados.domain.Prioridade;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import br.com.empresa.helpdesk.compartilhado.erros.ConflitoChamadoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ChamadoTransicoesTest {
  private static final Instant INICIO = Instant.parse("2026-09-24T12:00:00Z");
  private static final Instant DEPOIS = INICIO.plusSeconds(60);

  private Chamado novo() {
    return Chamado.abrir(
        "CH-2026-000001", "Acesso bloqueado", "Não consigo acessar.", 1L, null, 2L, null, INICIO);
  }

  @Test
  void assumirAtribuiAgenteEMoveChamadoAberto() {
    Chamado chamado = novo();
    chamado.assumir(10L, DEPOIS);
    assertThat(chamado.getResponsavelId()).isEqualTo(10L);
    assertThat(chamado.getStatus()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
    assertThat(chamado.getAtualizadoEm()).isEqualTo(DEPOIS);
    assertThatThrownBy(() -> chamado.assumir(11L, DEPOIS))
        .isInstanceOf(ConflitoChamadoException.class);
  }

  @Test
  void atribuirEAlterarCategoriaPrioridadeMantemIntegridade() {
    Chamado chamado = novo();
    chamado.atribuir(10L, DEPOIS);
    chamado.alterarCategoria(3L, DEPOIS);
    chamado.alterarPrioridade(Prioridade.ALTA, DEPOIS);
    assertThat(chamado.getStatus()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
    assertThat(chamado.getResponsavelId()).isEqualTo(10L);
    assertThat(chamado.getCategoriaId()).isEqualTo(3L);
    assertThat(chamado.getPrioridade()).isEqualTo(Prioridade.ALTA);
  }

  @Test
  void esperaRetornoEResolucaoSeguemMaquinaDeStatus() {
    Chamado chamado = novo();
    chamado.assumir(10L, DEPOIS);
    chamado.alterarStatus(StatusChamado.AGUARDANDO_USUARIO, null, DEPOIS);
    assertThat(chamado.getStatus()).isEqualTo(StatusChamado.AGUARDANDO_USUARIO);
    chamado.alterarStatus(StatusChamado.EM_ATENDIMENTO, null, DEPOIS);
    assertThat(chamado.getStatus()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
    chamado.alterarStatus(StatusChamado.RESOLVIDO, "Equipamento substituído", DEPOIS);
    assertThat(chamado.getSolucao()).isEqualTo("Equipamento substituído");
    assertThat(chamado.getResolvidoEm()).isEqualTo(DEPOIS);
    assertThatThrownBy(() -> chamado.atribuir(11L, DEPOIS))
        .isInstanceOf(ConflitoChamadoException.class);
  }

  @Test
  void rejeitaResolucaoSemSolucaoETransicoesInvalidas() {
    Chamado chamado = novo();
    assertThatThrownBy(() -> chamado.alterarStatus(StatusChamado.RESOLVIDO, "texto", DEPOIS))
        .isInstanceOf(ConflitoChamadoException.class);
    chamado.assumir(10L, DEPOIS);
    assertThatThrownBy(() -> chamado.alterarStatus(StatusChamado.RESOLVIDO, " ", DEPOIS))
        .isInstanceOf(RequisicaoInvalidaException.class);
    assertThat(chamado.getStatus()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
    assertThatThrownBy(() -> chamado.alterarStatus(StatusChamado.ABERTO, null, DEPOIS))
        .isInstanceOf(ConflitoChamadoException.class);
    assertThatThrownBy(() -> chamado.alterarStatus(StatusChamado.FECHADO, null, DEPOIS))
        .isInstanceOf(ConflitoChamadoException.class);
    chamado.alterarStatus(StatusChamado.RESOLVIDO, "Corrigido", DEPOIS);
    assertThatThrownBy(() -> chamado.alterarStatus(StatusChamado.ABERTO, null, DEPOIS))
        .isInstanceOf(ConflitoChamadoException.class);
  }

  @Test
  void reassumirChamadoSemResponsavelNaoDescartaEstadoDeEspera() {
    Chamado chamado = novo();
    chamado.assumir(10L, DEPOIS);
    chamado.alterarStatus(StatusChamado.AGUARDANDO_USUARIO, null, DEPOIS);
    chamado.atribuir(null, DEPOIS);
    chamado.assumir(11L, DEPOIS);
    assertThat(chamado.getResponsavelId()).isEqualTo(11L);
    assertThat(chamado.getStatus()).isEqualTo(StatusChamado.AGUARDANDO_USUARIO);
  }
}
