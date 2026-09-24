package br.com.empresa.helpdesk.admin.infra;

import br.com.empresa.helpdesk.admin.domain.Categoria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
  List<Categoria> findByAtivaTrueOrderByNomeAsc();

  boolean existsByIdAndAtivaTrue(Long id);
}
