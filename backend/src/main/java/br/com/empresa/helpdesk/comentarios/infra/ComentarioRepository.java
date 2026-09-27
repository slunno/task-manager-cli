package br.com.empresa.helpdesk.comentarios.infra;

import br.com.empresa.helpdesk.comentarios.domain.Comentario;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
  Page<Comentario> findByChamadoId(Long chamadoId, Pageable pageable);

  Page<Comentario> findByChamadoIdAndInternoFalse(Long chamadoId, Pageable pageable);

  Optional<Comentario> findByIdAndChamadoId(Long id, Long chamadoId);
}
