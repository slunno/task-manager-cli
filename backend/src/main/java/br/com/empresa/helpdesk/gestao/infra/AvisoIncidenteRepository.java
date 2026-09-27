package br.com.empresa.helpdesk.gestao.infra;

import br.com.empresa.helpdesk.gestao.domain.AvisoIncidente;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AvisoIncidenteRepository extends JpaRepository<AvisoIncidente, Long> {
  @Query(
      "select a from AvisoIncidente a where a.ativo = true and a.inicioEm <= :agora and (a.fimEm is null or a.fimEm > :agora) order by a.inicioEm desc")
  List<AvisoIncidente> vigentes(Instant agora);

  List<AvisoIncidente> findAllByOrderByCriadoEmDesc();
}
