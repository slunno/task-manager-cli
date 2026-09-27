package br.com.empresa.helpdesk.compartilhado;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.helpdesk.compartilhado.observabilidade.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {
  private final CorrelationIdFilter filter = new CorrelationIdFilter();

  @Test
  void aceitaIdentificadorSeguroELimpaContexto() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-ID", "chamado-123");
    var response = new MockHttpServletResponse();
    filter.doFilter(
        request,
        response,
        (req, res) -> assertThat(MDC.get("correlationId")).isEqualTo("chamado-123"));
    assertThat(response.getHeader("X-Request-ID")).isEqualTo("chamado-123");
    assertThat(MDC.get("correlationId")).isNull();
  }

  @Test
  void substituiIdentificadorInseguro() throws Exception {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Request-ID", "pessoa@empresa.com\nsegredo");
    var response = new MockHttpServletResponse();
    filter.doFilter(request, response, (req, res) -> {});
    assertThat(response.getHeader("X-Request-ID")).matches("[0-9a-f-]{36}");
  }
}
