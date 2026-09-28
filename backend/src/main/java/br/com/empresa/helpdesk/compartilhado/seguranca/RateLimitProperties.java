package br.com.empresa.helpdesk.compartilhado.seguranca;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("helpdesk.rate-limit")
public record RateLimitProperties(
    boolean enabled,
    Duration window,
    int login,
    int chamados,
    int comentarios,
    int anexos,
    int maxKeys) {
  public RateLimitProperties {
    if (window == null
        || window.isNegative()
        || window.isZero()
        || window.toMillis() < 1
        || login < 1
        || chamados < 1
        || comentarios < 1
        || anexos < 1
        || maxKeys < 1) {
      throw new IllegalArgumentException("Limites de taxa e janela devem ser positivos");
    }
  }
}
