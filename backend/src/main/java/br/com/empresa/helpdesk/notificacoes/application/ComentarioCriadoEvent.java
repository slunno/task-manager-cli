package br.com.empresa.helpdesk.notificacoes.application;

public record ComentarioCriadoEvent(Long chamadoId, Long autorId, boolean interno) {}
