package br.com.empresa.helpdesk.comentarios.api;

import java.time.Instant;

public record LinhaTempoItemResponse(
    Long id, String tipo, Long autorId, String texto, String status, Instant criadoEm) {}
