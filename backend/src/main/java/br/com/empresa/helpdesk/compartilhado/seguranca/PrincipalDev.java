package br.com.empresa.helpdesk.compartilhado.seguranca;

import java.io.Serializable;

public record PrincipalDev(Long id, String email) implements Serializable {}
