package br.com.empresa.helpdesk.chamados.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AssumirChamadoRequest(@NotNull @PositiveOrZero Long version) {}
