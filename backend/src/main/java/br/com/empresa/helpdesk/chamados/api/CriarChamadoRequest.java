package br.com.empresa.helpdesk.chamados.api;

import br.com.empresa.helpdesk.chamados.domain.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CriarChamadoRequest(
    @NotBlank @Size(max = 200) String titulo,
    @NotBlank @Size(max = 10000) String descricao,
    @NotNull @Positive Long categoriaId,
    Prioridade prioridadeSugerida,
    @Positive Long solicitanteId) {}
