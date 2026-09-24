package br.com.empresa.helpdesk.chamados.api;

import br.com.empresa.helpdesk.chamados.domain.Prioridade;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record ChamadoResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String numero,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String titulo,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String descricao,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long solicitanteId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) Long abertoPorId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long categoriaId,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Prioridade prioridade,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Prioridade prioridadeSugerida,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) StatusChamado status,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant criadoEm,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant atualizadoEm,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long version) {}
