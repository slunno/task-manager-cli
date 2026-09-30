package br.com.empresa.helpdesk.compartilhado.config;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
public class ValidacaoProducao {
  private final Environment ambiente;

  public ValidacaoProducao(Environment ambiente) {
    this.ambiente = ambiente;
  }

  @PostConstruct
  void validar() {
    if (!"s3".equals(ambiente.getProperty("helpdesk.storage.mode")))
      throw new IllegalStateException("Produção exige helpdesk.storage.mode=s3");
    exigir("helpdesk.storage.s3.endpoint", "HELPDESK_S3_ENDPOINT");
    exigir("helpdesk.storage.s3.access-key", "HELPDESK_S3_ACCESS_KEY");
    exigir("helpdesk.storage.s3.secret-key", "HELPDESK_S3_SECRET_KEY");
    exigir("helpdesk.storage.s3.bucket", "HELPDESK_S3_BUCKET");
    exigir("spring.mail.username", "SMTP_USERNAME");
    exigir("spring.mail.password", "SMTP_PASSWORD");
    if (Arrays.asList(ambiente.getActiveProfiles()).contains("supabase-auth")) {
      exigir("helpdesk.auth.supabase.url", "SUPABASE_URL");
      exigir("helpdesk.auth.supabase.publishable-key", "SUPABASE_PUBLISHABLE_KEY");
    }
    for (String chave :
        new String[] {
          "mail.smtp.auth", "mail.smtp.starttls.enable", "mail.smtp.starttls.required"
        }) {
      if (!ambiente.getProperty("spring.mail.properties." + chave, Boolean.class, false))
        throw new IllegalStateException(
            "Produção exige autenticação SMTP e STARTTLS obrigatório: " + chave);
    }
  }

  private void exigir(String chave, String variavel) {
    String valor = ambiente.getProperty(chave);
    if (valor == null || valor.isBlank())
      throw new IllegalStateException("Configuração de produção obrigatória: " + variavel);
  }
}
