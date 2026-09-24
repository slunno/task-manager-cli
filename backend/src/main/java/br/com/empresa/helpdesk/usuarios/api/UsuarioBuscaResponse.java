package br.com.empresa.helpdesk.usuarios.api;

import br.com.empresa.helpdesk.usuarios.domain.Perfil;

public record UsuarioBuscaResponse(Long id, String nome, String email, Perfil perfil) {}
