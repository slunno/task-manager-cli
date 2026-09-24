package br.com.empresa.helpdesk.compartilhado.erros;

import br.com.empresa.helpdesk.usuarios.application.UsuarioInativoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroApiHandler {
  @ExceptionHandler(UsuarioInativoException.class)
  ProblemDetail usuarioInativo() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN, "Usuário inativo. Contate a equipe de TI.");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail argumentoInvalido() {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Dados de identidade inválidos.");
  }
}
