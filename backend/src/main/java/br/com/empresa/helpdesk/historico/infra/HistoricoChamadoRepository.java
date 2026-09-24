package br.com.empresa.helpdesk.historico.infra;

import br.com.empresa.helpdesk.historico.domain.HistoricoChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoricoChamadoRepository extends JpaRepository<HistoricoChamado, Long> {
  Page<HistoricoChamado> findByChamadoId(Long chamadoId, Pageable pageable);
}
