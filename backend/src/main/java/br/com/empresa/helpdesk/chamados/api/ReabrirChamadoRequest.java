package br.com.empresa.helpdesk.chamados.api;

import jakarta.validation.constraints.NotNull;

public record ReabrirChamadoRequest(@NotNull Long version) {}
