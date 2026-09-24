package br.com.empresa.helpdesk.chamados;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
    properties = {
      "spring.flyway.enabled=true",
      "spring.jpa.hibernate.ddl-auto=validate",
      "spring.sql.init.mode=never"
    })
@ActiveProfiles("test")
class PostgresMigrationTest {
  @Container @ServiceConnection
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.15-alpine3.23");

  @Autowired private Flyway flyway;
  @Autowired private CategoriaRepository categorias;

  @Test
  void aplicaMigracoesEValidaMapeamento() {
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
    assertThat(categorias.findByAtivaTrueOrderByNomeAsc()).hasSize(5);
  }
}
