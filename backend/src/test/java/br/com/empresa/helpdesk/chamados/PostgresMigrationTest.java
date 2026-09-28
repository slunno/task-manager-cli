package br.com.empresa.helpdesk.chamados;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.chamados.application.FiltroFila;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import java.util.ArrayList;
import java.util.Collections;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    properties = {
      "spring.flyway.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.jpa.defer-datasource-initialization=false",
      "spring.sql.init.mode=never"
    })
@ActiveProfiles("test")
class PostgresMigrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.15-alpine3.23");

  @Autowired private Flyway flyway;
  @Autowired private CategoriaRepository categorias;
  @Autowired private UsuarioRepository usuarios;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ChamadoService chamados;
  @Autowired private br.com.empresa.helpdesk.sla.application.DashboardService dashboard;

  @Test
  void aplicaMigracoesEValidaMapeamento() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("10");
    assertThat(categorias.findByAtivaTrueOrderByNomeAsc()).hasSize(5);
  }

  @Test
  void filaP95AbaixoDe300MsComCemMilChamados() {
    Usuario agente = Usuario.novoFuncionario("Agente de desempenho", "desempenho@empresa.com");
    agente.alterarPerfil(Perfil.TI_AGENTE);
    usuarios.saveAndFlush(agente);
    Long categoriaId = categorias.findByAtivaTrueOrderByNomeAsc().getFirst().getId();
    jdbc.update(
        "insert into chamados (numero,titulo,descricao,solicitante_id,categoria_id,prioridade,status,canal,criado_em,atualizado_em,version) select 'CH-2026-' || lpad(gs::text, 6, '0'), 'Falha na rede', 'Conexão indisponível', ?, ?, 'MEDIA', 'ABERTO', 'PORTAL', now() - gs * interval '1 second', now(), 0 from generate_series(1,100000) gs",
        agente.getId(),
        categoriaId);
    jdbc.execute("analyze chamados");
    FiltroFila filtro =
        new FiltroFila(
            StatusChamado.ABERTO, null, null, null, null, null, null, null, false, false, false);
    ArrayList<Double> amostras = new ArrayList<>();
    ArrayList<Double> amostrasDashboard = new ArrayList<>();
    for (int i = 0; i < 40; i++) {
      long inicio = System.nanoTime();
      var pagina = chamados.fila(agente, filtro, 0, 10, "criadoEm,desc");
      double milissegundos = (System.nanoTime() - inicio) / 1_000_000.0;
      assertThat(pagina.content()).hasSize(10);
      if (i >= 10) amostras.add(milissegundos);
      inicio = System.nanoTime();
      assertThat(dashboard.consultar().totalChamados()).isEqualTo(100000);
      if (i >= 10) amostrasDashboard.add((System.nanoTime() - inicio) / 1_000_000.0);
    }
    Collections.sort(amostras);
    double p95 = amostras.get((int) Math.ceil(amostras.size() * 0.95) - 1);
    assertThat(p95).as("p95 da fila com 100 mil chamados (ms)").isLessThan(300.0);
    Collections.sort(amostrasDashboard);
    assertThat(amostrasDashboard.get((int) Math.ceil(amostrasDashboard.size() * 0.95) - 1))
        .as("p95 do dashboard com 100 mil chamados (ms)")
        .isLessThan(300.0);
  }
}
