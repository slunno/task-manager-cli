package br.com.empresa.helpdesk.historico.application;

import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.historico.api.HistoricoResponse;
import br.com.empresa.helpdesk.historico.domain.HistoricoChamado;
import br.com.empresa.helpdesk.historico.infra.HistoricoChamadoRepository;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoricoService {
  private final HistoricoChamadoRepository repository;

  public HistoricoService(HistoricoChamadoRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public void registrar(
      Long chamadoId, Long usuarioId, String campo, String antes, String depois, Instant agora) {
    repository.save(new HistoricoChamado(chamadoId, usuarioId, campo, antes, depois, agora));
  }

  @Transactional(readOnly = true)
  public PaginaResponse<HistoricoResponse> listar(Long chamadoId, int page, int size) {
    return PaginaResponse.de(
        repository
            .findByChamadoId(
                chamadoId,
                PageRequest.of(
                    page, size, Sort.by("criadoEm").ascending().and(Sort.by("id").ascending())))
            .map(HistoricoResponse::de));
  }
}
