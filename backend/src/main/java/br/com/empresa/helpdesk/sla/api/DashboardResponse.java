package br.com.empresa.helpdesk.sla.api;

import java.util.Map;

public record DashboardResponse(
    long totalChamados,
    long emAberto,
    long vencidos,
    long vencendo,
    Double tempoMedioResolucaoHoras,
    Map<String, Long> porStatus,
    Map<String, Long> porPrioridade) {}
