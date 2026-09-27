package br.com.empresa.helpdesk.anexos.infra;

import br.com.empresa.helpdesk.anexos.domain.Anexo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnexoRepository extends JpaRepository<Anexo, Long> {
  Page<Anexo> findByChamadoId(Long chamadoId, Pageable pageable);

  Page<Anexo> findByChamadoIdAndInternoFalse(Long chamadoId, Pageable pageable);
}
