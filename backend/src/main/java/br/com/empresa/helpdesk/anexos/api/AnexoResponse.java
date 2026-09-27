package br.com.empresa.helpdesk.anexos.api;

import br.com.empresa.helpdesk.anexos.domain.Anexo;
import java.time.Instant;

public record AnexoResponse(
    Long id,
    Long chamadoId,
    Long comentarioId,
    String nomeOriginal,
    String tipoMime,
    long tamanho,
    Long criadoPor,
    boolean interno,
    Instant criadoEm) {
  public static AnexoResponse de(Anexo anexo) {
    return new AnexoResponse(
        anexo.getId(),
        anexo.getChamadoId(),
        anexo.getComentarioId(),
        anexo.getNomeOriginal(),
        anexo.getTipoMime(),
        anexo.getTamanho(),
        anexo.getCriadoPor(),
        anexo.isInterno(),
        anexo.getCriadoEm());
  }
}
