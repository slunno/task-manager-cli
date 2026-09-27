package br.com.empresa.helpdesk.comentarios.api;

import br.com.empresa.helpdesk.comentarios.domain.Comentario;
import java.time.Instant;

public record ComentarioResponse(
    Long id, Long chamadoId, Long autorId, String texto, boolean interno, Instant criadoEm) {
  public static ComentarioResponse de(Comentario comentario) {
    return new ComentarioResponse(
        comentario.getId(),
        comentario.getChamadoId(),
        comentario.getAutorId(),
        comentario.getTexto(),
        comentario.isInterno(),
        comentario.getCriadoEm());
  }
}
