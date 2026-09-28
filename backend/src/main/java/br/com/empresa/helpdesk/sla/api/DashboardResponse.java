package br.com.empresa.helpdesk.sla.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.Map;

public record DashboardResponse(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalChamados,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long emAberto,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long vencidos,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long vencendo,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Double tempoMedioResolucaoHoras,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Map<String, Long> porStatus,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Map<String, Long> porPrioridade,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Map<String, Long> porCategoria,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true)
        Double percentualSlaCumprido,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long resolvidos,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long resolvidosSemTempoUtil,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate desde,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate ate) {}
