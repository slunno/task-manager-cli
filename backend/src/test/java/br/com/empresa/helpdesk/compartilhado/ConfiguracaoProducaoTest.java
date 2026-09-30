package br.com.empresa.helpdesk.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.helpdesk.anexos.application.StorageService;
import br.com.empresa.helpdesk.anexos.infra.LocalStorageService;
import br.com.empresa.helpdesk.anexos.infra.S3StorageService;
import br.com.empresa.helpdesk.compartilhado.config.ValidacaoProducao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ConfiguracaoProducaoTest {
  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withInitializer(new ConfigDataApplicationContextInitializer())
          .withUserConfiguration(Config.class)
          .withPropertyValues(
              "spring.profiles.active=prod",
              "HELPDESK_S3_ENDPOINT=http://localhost:9000",
              "HELPDESK_S3_ACCESS_KEY=ficticio",
              "HELPDESK_S3_SECRET_KEY=ficticio-secret",
              "HELPDESK_S3_BUCKET=anexos-teste",
              "SMTP_USERNAME=ficticio",
              "SMTP_PASSWORD=ficticio");

  @Configuration(proxyBeanMethods = false)
  @Import({S3StorageService.class, LocalStorageService.class, ValidacaoProducao.class})
  static class Config {}

  @ParameterizedTest
  @ValueSource(
      strings = {
        "HELPDESK_S3_ENDPOINT",
        "HELPDESK_S3_ACCESS_KEY",
        "HELPDESK_S3_SECRET_KEY",
        "HELPDESK_S3_BUCKET",
        "SMTP_USERNAME",
        "SMTP_PASSWORD"
      })
  void prodFalhaSeConfiguracaoObrigatoriaFaltar(String variavel) {
    runner
        .withPropertyValues(variavel + "=")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseMessage(
                      (variavel.startsWith("HELPDESK_S3")
                              ? "Configuração S3 obrigatória: "
                              : "Configuração de produção obrigatória: ")
                          + variavel);
            });
  }

  @Test
  void prodSelecionaS3SemConectarAoStorageDuranteInicializacao() {
    runner.run(
        context -> {
          assertThat(context).hasNotFailed().hasSingleBean(StorageService.class);
          assertThat(context.getBean(StorageService.class)).isInstanceOf(S3StorageService.class);
        });
  }

  @Test
  void prodRejeitaFallbackLocalETlsDesativado() {
    runner
        .withPropertyValues("helpdesk.storage.mode=local")
        .run(context -> assertThat(context).hasFailed());
    runner
        .withPropertyValues("SMTP_STARTTLS_ENABLE=false")
        .run(context -> assertThat(context).hasFailed());
  }

  @ParameterizedTest
  @ValueSource(strings = {"SUPABASE_URL", "SUPABASE_PUBLISHABLE_KEY"})
  void prodComSupabaseAuthExigeConfiguracaoDeLogin(String variavel) {
    runner
        .withPropertyValues(
            "spring.profiles.active=prod,supabase-auth",
            "SUPABASE_URL=https://auth.example",
            "SUPABASE_PUBLISHABLE_KEY=sb_publishable_test",
            variavel + "=")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseMessage("Configuração de produção obrigatória: " + variavel);
            });
  }

  @Test
  void prodComSupabaseAuthAceitaConfiguracaoCompleta() {
    runner
        .withPropertyValues(
            "spring.profiles.active=prod,supabase-auth",
            "SUPABASE_URL=https://auth.example",
            "SUPABASE_PUBLISHABLE_KEY=sb_publishable_test")
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void devMantemStorageLocalSemCredenciais() {
    runner
        .withPropertyValues(
            "spring.profiles.active=dev", "HELPDESK_S3_ACCESS_KEY=", "SMTP_USERNAME=")
        .run(
            context -> {
              assertThat(context).hasNotFailed().hasSingleBean(StorageService.class);
              assertThat(context.getBean(StorageService.class))
                  .isInstanceOf(LocalStorageService.class);
            });
  }
}
