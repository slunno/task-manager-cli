package br.com.empresa.helpdesk.usuarios.application;

public class UsuarioInativoException extends RuntimeException {
  public UsuarioInativoException() {
    super("Usuário inativo");
  }
}
