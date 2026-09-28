package br.com.empresa.helpdesk.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.empresa.helpdesk.compartilhado.retencao.RetencaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
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
class HistoricoPostgresTest {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.15-alpine3.23");

  @Autowired JdbcTemplate jdbc;
  @Autowired RetencaoService retencao;
  @Autowired PlatformTransactionManager transacoes;
  long chamado;

  @BeforeEach
  void preparar() {
    Long usuario =
        jdbc.queryForObject(
            "insert into usuarios(nome,email,perfil) values ('Pessoa fictícia', ?, 'FUNCIONARIO') returning id",
            Long.class,
            java.util.UUID.randomUUID() + "@exemplo.local");
    chamado =
        jdbc.queryForObject(
            "insert into chamados(numero,titulo,descricao,solicitante_id,categoria_id,status,fechado_em) values (?, 'Título fictício', 'Conteúdo fictício', ?, 1, 'FECHADO', timestamp '1990-01-01 00:00:00') returning id",
            Long.class,
            "T-" + java.util.UUID.randomUUID().toString().substring(0, 12),
            usuario);
    jdbc.update(
        "insert into historico_chamado(chamado_id,usuario_id,campo,valor_novo) values (?, ?, 'status', 'FECHADO')",
        chamado,
        usuario);
  }

  @Test
  void updateEDeleteAvulsosFalhamSemAlterarHistorico() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update historico_chamado set valor_novo='alterado' where chamado_id=?",
                    chamado))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("imutável");
    assertThatThrownBy(
            () -> jdbc.update("delete from historico_chamado where chamado_id=?", chamado))
        .isInstanceOf(DataAccessException.class)
        .rootCause()
        .hasMessageContaining("imutável");
    assertThat(
            jdbc.queryForObject(
                "select valor_novo from historico_chamado where chamado_id=?",
                String.class,
                chamado))
        .isEqualTo("FECHADO");
  }

  @Test
  void retencaoApagaEAnonimizaSemDeixarContextoHabilitado() {
    Long recente =
        jdbc.queryForObject(
            "insert into chamados(numero,titulo,descricao,solicitante_id,categoria_id) select ?, 'Recente', 'Conteúdo', solicitante_id, categoria_id from chamados where id=? returning id",
            Long.class,
            "R-" + chamado,
            chamado);
    jdbc.update(
        "insert into historico_chamado(chamado_id,campo,valor_novo) values (?, 'status', 'ABERTO')",
        recente);
    new TransactionTemplate(transacoes)
        .executeWithoutResult(
            status -> {
              assertThat(retencao.anonimizarLote()).isPositive();
              assertThat(
                      jdbc.queryForObject(
                          "select current_setting('helpdesk.retencao', true)", String.class))
                  .isEqualTo("off");
              assertThat(
                      jdbc.queryForObject(
                          "select count(*) from historico_chamado where chamado_id=?",
                          Long.class,
                          chamado))
                  .isZero();
              assertThat(
                      jdbc.queryForObject(
                          "select titulo from chamados where id=?", String.class, chamado))
                  .isEqualTo("Chamado anonimizado");
              assertThat(
                      jdbc.queryForObject(
                          "select email from usuarios where id=(select solicitante_id from chamados where id=?)",
                          String.class,
                          chamado))
                  .isEqualTo("anonimo@helpdesk.invalid");
            });
    assertThatThrownBy(
            () -> jdbc.update("delete from historico_chamado where chamado_id=?", recente))
        .isInstanceOf(DataAccessException.class);
  }

  @Test
  void contextoLocalNaoPermiteUpdateERollbackPreservaDados() {
    assertThatThrownBy(
            () ->
                new TransactionTemplate(transacoes)
                    .executeWithoutResult(
                        status -> {
                          jdbc.execute("set local helpdesk.retencao = 'on'");
                          jdbc.update(
                              "update historico_chamado set valor_novo='alterado' where chamado_id=?",
                              chamado);
                        }))
        .isInstanceOf(DataAccessException.class);
    new TransactionTemplate(transacoes)
        .executeWithoutResult(
            status -> {
              jdbc.execute("set local helpdesk.retencao = 'on'");
              jdbc.update("delete from historico_chamado where chamado_id=?", chamado);
              status.setRollbackOnly();
            });
    assertThat(
            jdbc.queryForObject(
                "select count(*) from historico_chamado where chamado_id=?", Long.class, chamado))
        .isEqualTo(1);
    assertThatThrownBy(
            () -> jdbc.update("delete from historico_chamado where chamado_id=?", chamado))
        .isInstanceOf(DataAccessException.class);
  }
}
