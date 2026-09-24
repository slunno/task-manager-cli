package br.com.empresa.helpdesk.compartilhado.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

public final class ProblemaSeguranca {
  private ProblemaSeguranca() {}

  public static void escrever(
      HttpServletResponse response, HttpStatus status, String detalhe, ObjectMapper mapper)
      throws IOException {
    ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setHeader("Cache-Control", "no-store");
    mapper.writeValue(response.getWriter(), problema);
  }
}
