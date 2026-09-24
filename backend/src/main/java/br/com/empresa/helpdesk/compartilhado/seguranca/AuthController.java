package br.com.empresa.helpdesk.compartilhado.seguranca;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
  private final String modo;

  public AuthController(@Value("${helpdesk.auth.mode:oidc}") String modo) {
    this.modo = modo;
  }

  @GetMapping("/api/v1/auth/config")
  public ResponseEntity<ConfigResponse> config() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            new ConfigResponse(
                modo, "oidc".equals(modo) ? "/oauth2/authorization/corporativo" : null));
  }

  @GetMapping("/api/v1/auth/csrf")
  public ResponseEntity<CsrfResponse> csrf(HttpServletRequest request) {
    CsrfToken csrf = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(new CsrfResponse(csrf.getToken()));
  }

  public record ConfigResponse(String modo, String urlLogin) {}

  public record CsrfResponse(String token) {}
}
