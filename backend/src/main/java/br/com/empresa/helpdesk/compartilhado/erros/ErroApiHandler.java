package br.com.empresa.helpdesk.compartilhado.erros;

import br.com.empresa.helpdesk.usuarios.application.UsuarioInativoException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ErroApiHandler {
  @ExceptionHandler({ConflitoChamadoException.class, OptimisticLockingFailureException.class})
  ProblemDetail conflito(Exception ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex instanceof ConflitoChamadoException
            ? ex.getMessage()
            : "O chamado foi alterado por outra pessoa. Atualize a página e tente novamente.");
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  ProblemDetail naoEncontrado(RecursoNaoEncontradoException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(RequisicaoInvalidaException.class)
  ProblemDetail requisicaoInvalida(RequisicaoInvalidaException ex) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validacao(MethodArgumentNotValidException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST, "Dados inválidos na requisição.");
  }

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
