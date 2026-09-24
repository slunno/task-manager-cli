package br.com.empresa.helpdesk.historico.api;

import br.com.empresa.helpdesk.historico.domain.HistoricoChamado;
import java.time.Instant;

public record HistoricoResponse(
    Long id,
    Long usuarioId,
    String campo,
    String valorAnterior,
    String valorNovo,
    Instant criadoEm) {
  public static HistoricoResponse de(HistoricoChamado registro) {
    return new HistoricoResponse(
        registro.getId(),
        registro.getUsuarioId(),
        registro.getCampo(),
        registro.getValorAnterior(),
        registro.getValorNovo(),
        registro.getCriadoEm());
  }
}
