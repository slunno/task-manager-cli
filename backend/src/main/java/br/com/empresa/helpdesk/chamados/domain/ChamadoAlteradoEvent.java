package br.com.empresa.helpdesk.chamados.domain;

import java.time.Instant;

public record ChamadoAlteradoEvent(
    Long chamadoId,
    Long usuarioId,
    String campo,
    String valorAnterior,
    String valorNovo,
    Instant ocorridoEm) {}
