package br.com.empresa.helpdesk.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ConfiguracaoSupabaseTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .withPropertyValues(
              "spring.profiles.active=dev,supabase", "SUPABASE_DB_PASSWORD=ficticia-apenas-teste");

  @Test
  void usaSchemaPrivadoSslEFlywaySemBaselineAutomatico() {
    runner.run(
        context -> {
          var env = context.getEnvironment();
          assertThat(env.getRequiredProperty("spring.datasource.url"))
              .isEqualTo(
                  "jdbc:postgresql://db.evsmgbziifqbyahvcwsc.supabase.co:5432/postgres?sslmode=require&currentSchema=helpdesk");
          assertThat(env.getRequiredProperty("spring.datasource.username")).isEqualTo("postgres");
          assertThat(env.getRequiredProperty("spring.datasource.password"))
              .isEqualTo("ficticia-apenas-teste");
          HikariConfig pool =
              Binder.get(env)
                  .bind("spring.datasource.hikari", Bindable.of(HikariConfig.class))
                  .get();
          assertThat(pool.getSchema()).isEqualTo("helpdesk");
          assertThat(pool.getMaximumPoolSize()).isEqualTo(5);
          assertThat(pool.getMinimumIdle()).isEqualTo(1);
          assertThat(env.getRequiredProperty("spring.jpa.properties.hibernate.default_schema"))
              .isEqualTo("helpdesk");
          assertThat(env.getRequiredProperty("spring.flyway.default-schema")).isEqualTo("helpdesk");
          assertThat(env.getRequiredProperty("spring.flyway.baseline-on-migrate", Boolean.class))
              .isFalse();
          assertThat(env.getRequiredProperty("spring.flyway.clean-disabled", Boolean.class))
              .isTrue();
          assertThat(env.getRequiredProperty("helpdesk.auth.mode")).isEqualTo("dev");
        });
  }

  @Test
  void aceitaSessionPoolerComSenhaSeparadaSemAlterarAutenticacaoProd() {
    runner
        .withPropertyValues(
            "spring.profiles.active=prod,supabase",
            "SUPABASE_DB_JDBC_URL=jdbc:postgresql://pooler.exemplo.invalid:5432/postgres?sslmode=require&currentSchema=helpdesk",
            "SUPABASE_DB_USER=postgres.projeto-teste",
            "SUPABASE_DB_PASSWORD=senha-ficticia?&#$com-caracteres",
            "SUPABASE_DB_POOL_SIZE=3")
        .run(
            context -> {
              var env = context.getEnvironment();
              assertThat(env.getRequiredProperty("spring.datasource.url"))
                  .doesNotContain("senha-ficticia")
                  .contains("pooler.exemplo.invalid:5432", "sslmode=require");
              assertThat(env.getRequiredProperty("spring.datasource.username"))
                  .isEqualTo("postgres.projeto-teste");
              assertThat(env.getRequiredProperty("spring.datasource.password"))
                  .isEqualTo("senha-ficticia?&#$com-caracteres");
              assertThat(env.getRequiredProperty("helpdesk.auth.mode")).isEqualTo("oidc");
              assertThat(env.getRequiredProperty("helpdesk.storage.mode")).isEqualTo("s3");
            });
  }

  @Test
  void naoForneceSenhaPadrao() {
    new ApplicationContextRunner()
        .withInitializer(new ConfigDataApplicationContextInitializer())
        .withPropertyValues("spring.profiles.active=dev,supabase")
        .run(
            context ->
                assertThatThrownBy(
                        () ->
                            context
                                .getEnvironment()
                                .getRequiredProperty("spring.datasource.password"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("SUPABASE_DB_PASSWORD"));
  }
}
