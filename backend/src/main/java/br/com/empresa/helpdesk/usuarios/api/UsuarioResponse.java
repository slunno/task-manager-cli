package br.com.empresa.helpdesk.usuarios.api;

import br.com.empresa.helpdesk.usuarios.domain.Perfil;

public record UsuarioResponse(Long id, String nome, String email, Perfil perfil) {}
