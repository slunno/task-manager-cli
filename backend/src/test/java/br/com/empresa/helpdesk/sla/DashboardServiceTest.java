package br.com.empresa.helpdesk.sla;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.helpdesk.admin.domain.Categoria;
import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import br.com.empresa.helpdesk.chamados.domain.Chamado;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import br.com.empresa.helpdesk.chamados.infra.ChamadoRepository;
import br.com.empresa.helpdesk.sla.application.DashboardService;
import br.com.empresa.helpdesk.sla.application.SlaService;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DashboardServiceTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired SlaService sla;
  @Autowired ChamadoRepository chamados;
  @Autowired UsuarioRepository usuarios;
  @Autowired CategoriaRepository categorias;

  @Test
  void agregaPeriodoSlaCategoriasEHorasUteisComFimDeSemanaEFeriado() {
    Instant agora = Instant.parse("2026-10-07T12:00:00Z");
    var dashboard = new DashboardService(jdbc, Clock.fixed(agora, ZoneOffset.UTC));
    jdbc.update("delete from expediente");
    jdbc.update("delete from feriados");
    for (int dia = 1; dia <= 5; dia++)
      jdbc.update(
          "insert into expediente (dia_semana,inicio,fim,ativo) values (?,time '09:00:00',time '17:00:00',true)",
          dia);
    jdbc.update(
        "insert into feriados (data,descricao) values (date '2026-10-05','Feriado fictício')");
    var pessoa =
        usuarios.saveAndFlush(
            Usuario.novoFuncionario("Pessoa fictícia", "dashboard-c5@example.com"));
    var categoriaA = categorias.saveAndFlush(new Categoria("Categoria A C5"));
    var categoriaB = categorias.saveAndFlush(new Categoria("Categoria B C5"));
    Instant sexta = Instant.parse("2026-10-02T19:00:00Z");
    var dentro =
        resolvido(
            "CH-C5-1",
            pessoa.getId(),
            categoriaA.getId(),
            sexta,
            Instant.parse("2026-10-06T14:00:00Z"),
            Instant.parse("2026-10-06T15:00:00Z"));
    var fora =
        resolvido(
            "CH-C5-2",
            pessoa.getId(),
            categoriaB.getId(),
            sexta,
            Instant.parse("2026-10-06T16:00:00Z"),
            Instant.parse("2026-10-06T14:00:00Z"));
    assertThat(dentro.getResolucaoMinutosUteis()).isEqualTo(180);
    assertThat(fora.getResolucaoMinutosUteis()).isEqualTo(300);
    var aberto =
        Chamado.abrir(
            "CH-C5-3",
            "Chamado fictício",
            "Descrição fictícia",
            pessoa.getId(),
            null,
            categoriaA.getId(),
            null,
            Instant.parse("2026-10-04T13:00:00Z"));
    aberto.definirPrazos(agora.plusSeconds(1800), agora.plusSeconds(1800));
    chamados.saveAndFlush(aberto);
    resolvido(
        "CH-C5-4",
        pessoa.getId(),
        categoriaA.getId(),
        Instant.parse("2026-09-01T12:00:00Z"),
        sexta,
        sexta);
    var indicadores = dashboard.consultar(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 6));
    assertThat(indicadores.totalChamados()).isEqualTo(3);
    assertThat(indicadores.resolvidos()).isEqualTo(2);
    assertThat(indicadores.vencendo()).isEqualTo(1);
    assertThat(indicadores.percentualSlaCumprido()).isEqualTo(50.0);
    assertThat(indicadores.tempoMedioResolucaoHoras()).isEqualTo(4.0);
    assertThat(indicadores.resolvidosSemTempoUtil()).isZero();
    assertThat(indicadores.porCategoria())
        .containsEntry("Categoria A C5", 2L)
        .containsEntry("Categoria B C5", 1L);
    assertThat(dashboard.consultar().desde()).isEqualTo(LocalDate.of(2026, 9, 8));
    dentro.reabrir(agora);
    assertThat(dentro.getResolucaoMinutosUteis()).isNull();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> dashboard.consultar(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 1)))
        .isInstanceOf(
            br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException.class);
  }

  private Chamado resolvido(
      String numero, Long pessoa, Long categoria, Instant inicio, Instant fim, Instant prazo) {
    var chamado =
        Chamado.abrir(
            numero,
            "Chamado fictício",
            "Descrição fictícia",
            pessoa,
            null,
            categoria,
            null,
            inicio);
    chamado.assumir(pessoa, inicio);
    chamado.definirPrazos(inicio.plusSeconds(3600), prazo);
    chamado.alterarStatus(StatusChamado.RESOLVIDO, "Solução fictícia", fim);
    sla.registrarResolucao(chamado, fim);
    return chamados.saveAndFlush(chamado);
  }
}
