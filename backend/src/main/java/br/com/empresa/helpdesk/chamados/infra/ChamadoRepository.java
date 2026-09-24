package br.com.empresa.helpdesk.chamados.infra;

import br.com.empresa.helpdesk.chamados.domain.Chamado;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
  Page<Chamado> findBySolicitanteId(Long solicitanteId, Pageable pageable);

  Optional<Chamado> findByIdAndSolicitanteId(Long id, Long solicitanteId);
}
