package br.com.empresa.helpdesk.compartilhado.seguranca;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitFilterTest {
  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void excederRetornaProblemDetailsERetryAfterEJanelaRenova() throws Exception {
    var clock = new MutableClock();
    var filter = filter(clock, true, 1, 100);
    assertThat(request(filter, "/api/v1/auth/password/login", "10.0.0.1").getStatus())
        .isEqualTo(200);
    var exceeded = request(filter, "/api/v1/auth/password/login", "10.0.0.1");
    assertThat(exceeded.getStatus()).isEqualTo(429);
    assertThat(exceeded.getHeader("Retry-After")).isEqualTo("60");
    assertThat(exceeded.getContentType()).startsWith("application/problem+json");
    assertThat(exceeded.getContentAsString()).doesNotContain("10.0.0.1");
    clock.now.addAndGet(60_000);
    assertThat(request(filter, "/api/v1/auth/password/login", "10.0.0.1").getStatus())
        .isEqualTo(200);
  }

  @Test
  void usuariosCompartilhandoIpPossuemLimitesIndependentes() throws Exception {
    var filter = filter(new MutableClock(), true, 1, 100);
    authenticate(1L);
    assertThat(request(filter, "/api/v1/chamados/1/anexos", "10.0.0.1").getStatus()).isEqualTo(200);
    assertThat(request(filter, "/api/v1/chamados/2/anexos", "10.0.0.1").getStatus()).isEqualTo(429);
    authenticate(2L);
    assertThat(request(filter, "/api/v1/chamados/1/anexos", "10.0.0.1").getStatus()).isEqualTo(200);
    assertThat(request(filter, "/api/v1/chamados/1/comentarios", "10.0.0.1").getStatus())
        .isEqualTo(200);
  }

  @Test
  void encaminhamentoNaoConfiavelNaoTrocaIdentidadeELimitacaoPodeSerDesligada() throws Exception {
    var filter = filter(new MutableClock(), true, 1, 100);
    assertThat(request(filter, "/api/v1/auth/dev/login", "10.0.0.1").getStatus()).isEqualTo(200);
    assertThat(request(filter, "/api/v1/auth/dev/login", "10.0.0.1").getStatus()).isEqualTo(429);
    var disabled = filter(new MutableClock(), false, 1, 100);
    for (int i = 0; i < 5; i++)
      assertThat(request(disabled, "/api/v1/auth/dev/login", "10.0.0.1").getStatus())
          .isEqualTo(200);
  }

  @Test
  void memoriaLimitadaRecusaNovasChavesERemoveExpiradas() throws Exception {
    var clock = new MutableClock();
    var filter = filter(clock, true, 1, 1);
    assertThat(request(filter, "/api/v1/chamados", "10.0.0.1").getStatus()).isEqualTo(200);
    assertThat(request(filter, "/api/v1/chamados", "10.0.0.2").getStatus()).isEqualTo(429);
    clock.now.addAndGet(60_000);
    assertThat(request(filter, "/api/v1/chamados", "10.0.0.2").getStatus()).isEqualTo(200);
  }

  @Test
  void concorrenciaNaoPermiteUltrapassarLimite() {
    var filter = filter(new MutableClock(), true, 10, 100);
    long allowed =
        IntStream.range(0, 50)
            .parallel()
            .filter(
                i -> {
                  try {
                    return request(filter, "/api/v1/auth/password/login", "10.0.0.1").getStatus()
                        == 200;
                  } catch (Exception ex) {
                    throw new RuntimeException(ex);
                  }
                })
            .count();
    assertThat(allowed).isEqualTo(10);
  }

  private void authenticate(Long id) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new PrincipalDev(id, "fixture@example.invalid"), null, List.of()));
  }

  private RateLimitFilter filter(Clock clock, boolean enabled, int limit, int maxKeys) {
    return new RateLimitFilter(
        new RateLimitProperties(
            enabled, Duration.ofMinutes(1), limit, limit, limit, limit, maxKeys),
        clock,
        new ObjectMapper());
  }

  private MockHttpServletResponse request(RateLimitFilter filter, String path, String ip)
      throws Exception {
    var request = new MockHttpServletRequest("POST", path);
    request.setServletPath(path);
    request.setRemoteAddr(ip);
    request.addHeader("X-Forwarded-For", "spoof-" + System.nanoTime());
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, (req, res) -> {});
    return response;
  }

  private static class MutableClock extends Clock {
    final AtomicLong now = new AtomicLong(1_000_000);

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return Instant.ofEpochMilli(now.get());
    }
  }
}
