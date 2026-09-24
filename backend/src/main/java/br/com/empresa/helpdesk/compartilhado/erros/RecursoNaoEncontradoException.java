package br.com.empresa.helpdesk.compartilhado.erros;

public class RecursoNaoEncontradoException extends RuntimeException {
  public RecursoNaoEncontradoException(String mensagem) {
    super(mensagem);
  }
}
