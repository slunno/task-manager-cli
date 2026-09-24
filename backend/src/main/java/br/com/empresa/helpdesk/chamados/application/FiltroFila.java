package br.com.empresa.helpdesk.chamados.application;

import br.com.empresa.helpdesk.chamados.domain.Prioridade;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import java.time.Instant;

public record FiltroFila(
    StatusChamado status,
    Prioridade prioridade,
    Long responsavelId,
    Long categoriaId,
    Long setorId,
    Instant desde,
    Instant ate,
    String texto,
    boolean semResponsavel,
    boolean meus,
    boolean slaVencendo) {}
