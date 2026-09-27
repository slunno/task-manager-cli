package br.com.empresa.helpdesk.comentarios.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CriarComentarioRequest(@NotBlank @Size(max = 10000) String texto, boolean interno) {}
