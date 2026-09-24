package br.com.empresa.helpdesk.compartilhado.erros;

public class ConflitoChamadoException extends RuntimeException {
  public ConflitoChamadoException(String mensagem) {
    super(mensagem);
  }
}
