package br.com.empresa.helpdesk.conhecimento.infra;

import br.com.empresa.helpdesk.conhecimento.domain.FiltroSalvo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiltroSalvoRepository extends JpaRepository<FiltroSalvo, Long> {
  List<FiltroSalvo> findByUsuarioIdOrderByNomeAsc(Long usuarioId);

  Optional<FiltroSalvo> findByIdAndUsuarioId(Long id, Long usuarioId);

  boolean existsByUsuarioIdAndNome(Long usuarioId, String nome);

  long countByUsuarioId(Long usuarioId);
}
