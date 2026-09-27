package br.com.empresa.helpdesk.chamados.infra;

import br.com.empresa.helpdesk.chamados.domain.Chamado;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChamadoRepository
    extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {
  Page<Chamado> findBySolicitanteId(Long solicitanteId, Pageable pageable);

  List<Chamado> findTop100ByStatusAndResolvidoEmBeforeOrderByResolvidoEmAsc(
      br.com.empresa.helpdesk.chamados.domain.StatusChamado status, Instant limite);

  @org.springframework.data.jpa.repository.Query(
      "select c from Chamado c where c.status not in (br.com.empresa.helpdesk.chamados.domain.StatusChamado.RESOLVIDO, br.com.empresa.helpdesk.chamados.domain.StatusChamado.FECHADO) and c.slaPausadoEm is null and c.responsavelId is not null and c.prazoResolucao between :inicio and :fim")
  List<Chamado> vencendoSla(Instant inicio, Instant fim);

  Optional<Chamado> findByIdAndSolicitanteId(Long id, Long solicitanteId);

  List<Chamado> findTop50ByChamadoPrincipalIdOrderByCriadoEmDesc(Long chamadoPrincipalId);
}
