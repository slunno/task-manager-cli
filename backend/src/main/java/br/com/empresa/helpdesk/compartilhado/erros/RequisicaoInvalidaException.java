package br.com.empresa.helpdesk.compartilhado.erros;

public class RequisicaoInvalidaException extends RuntimeException {
  public RequisicaoInvalidaException(String mensagem) {
    super(mensagem);
  }
}
