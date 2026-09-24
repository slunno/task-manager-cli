package br.com.empresa.helpdesk.chamados.api;

import br.com.empresa.helpdesk.chamados.domain.Prioridade;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AtualizarChamadoRequest(
    @NotNull @PositiveOrZero Long version,
    StatusChamado status,
    Prioridade prioridade,
    @Positive Long categoriaId,
    @Positive Long responsavelId,
    Boolean removerResponsavel,
    @Size(max = 10000) String solucao) {}
