package br.com.empresa.helpdesk.compartilhado.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Janela fixa por operação e identidade, com armazenamento limitado por instância. */
public class RateLimitFilter extends OncePerRequestFilter {
  private final RateLimitProperties properties;
  private final Clock clock;
  private final ObjectMapper mapper;
  private final Map<Key, Window> windows = new HashMap<>();
  private long nextCleanup;

  public RateLimitFilter(RateLimitProperties properties, Clock clock, ObjectMapper mapper) {
    this.properties = properties;
    this.clock = clock;
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String operation = operation(request.getMethod(), request.getServletPath());
    if (!properties.enabled() || operation == null) {
      chain.doFilter(request, response);
      return;
    }
    var auth = SecurityContextHolder.getContext().getAuthentication();
    String identity = "ip:" + request.getRemoteAddr();
    if (!"login".equals(operation)
        && auth != null
        && auth.isAuthenticated()
        && !(auth instanceof AnonymousAuthenticationToken)) {
      identity =
          auth.getPrincipal() instanceof PrincipalDev principal
              ? "user:" + principal.id()
              : "user:" + auth.getName();
    }
    long retry = acquire(new Key(operation, identity), limit(operation));
    if (retry > 0) {
      response.setHeader("Retry-After", Long.toString(retry));
      ProblemaSeguranca.escrever(
          response,
          HttpStatus.TOO_MANY_REQUESTS,
          "Muitas tentativas. Aguarde antes de tentar novamente.",
          mapper);
      return;
    }
    chain.doFilter(request, response);
  }

  private synchronized long acquire(Key key, int limit) {
    long now = clock.millis();
    long duration = properties.window().toMillis();
    if (now >= nextCleanup) {
      windows.values().removeIf(window -> window.expires <= now);
      nextCleanup = now + Math.min(duration, 60_000);
    }
    Window window = windows.get(key);
    if (window == null || window.expires <= now) {
      if (window == null && windows.size() >= properties.maxKeys()) {
        // Recolher janelas expiradas antes de recusar uma identidade nova por capacidade.
        windows.values().removeIf(entry -> entry.expires <= now);
        if (windows.size() >= properties.maxKeys()) return Math.max(1, (duration + 999) / 1000);
      }
      window = new Window(now + duration);
      windows.put(key, window);
    }
    if (window.used >= limit) return Math.max(1, (window.expires - now + 999) / 1000);
    window.used++;
    return 0;
  }

  private int limit(String operation) {
    return switch (operation) {
      case "login" -> properties.login();
      case "chamados" -> properties.chamados();
      case "comentarios" -> properties.comentarios();
      default -> properties.anexos();
    };
  }

  private String operation(String method, String path) {
    if ("GET".equals(method)
        && (path.startsWith("/oauth2/authorization/") || path.startsWith("/login/oauth2/code/")))
      return "login";
    if (!"POST".equals(method)) return null;
    if (path.equals("/api/v1/auth/dev/login") || path.equals("/api/v1/auth/password/login"))
      return "login";
    if (path.equals("/api/v1/chamados") || path.equals("/api/v1/chamados/")) return "chamados";
    if (path.matches("/api/v1/chamados/[0-9]+/comentarios/?")) return "comentarios";
    if (path.matches("/api/v1/chamados/[0-9]+/anexos/?")) return "anexos";
    return null;
  }

  private record Key(String operation, String identity) {}

  private static class Window {
    final long expires;
    int used;

    Window(long expires) {
      this.expires = expires;
    }
  }
}
