package br.com.empresa.helpdesk.compartilhado.seguranca;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Profile("supabase-auth")
public class SupabasePasswordClient {
  private final RestClient client;

  public SupabasePasswordClient(
      @Value("${helpdesk.auth.supabase.url}") String url,
      @Value("${helpdesk.auth.supabase.publishable-key}") String key) {
    URI uri = URI.create(url);
    if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
      throw new IllegalStateException("Supabase Auth exige uma URL HTTPS sem credenciais");
    }
    if (key.isBlank() || key.startsWith("sb_secret_")) {
      throw new IllegalStateException("Supabase Auth exige a chave publicável");
    }
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
    factory.setReadTimeout(Duration.ofSeconds(10));
    client =
        RestClient.builder()
            .baseUrl(url)
            .defaultHeader("apikey", key)
            .requestFactory(factory)
            .build();
  }

  SupabasePasswordClient(RestClient client) {
    this.client = client;
  }

  public String autenticar(String email, String senha) {
    try {
      var result =
          client
              .post()
              .uri("/auth/v1/token?grant_type=password")
              .body(Map.of("email", email, "password", senha))
              .retrieve()
              .body(TokenResponse.class);
      if (result == null
          || result.user() == null
          || result.user().id() == null
          || result.user().emailConfirmedAt() == null
          || result.user().email() == null
          || !email.equalsIgnoreCase(result.user().email())) {
        throw new LoginRecusadoException();
      }
      // Tokens e metadados não são gravados na sessão nem usados para definir permissões.
      return result.user().email();
    } catch (RestClientException ex) {
      throw new LoginRecusadoException();
    }
  }

  record TokenResponse(AuthUser user) {}

  record AuthUser(
      String id, String email, @JsonProperty("email_confirmed_at") String emailConfirmedAt) {}

  public static class LoginRecusadoException extends RuntimeException {
    public LoginRecusadoException() {
      super("E-mail ou senha inválidos, ou acesso indisponível.");
    }
  }
}
