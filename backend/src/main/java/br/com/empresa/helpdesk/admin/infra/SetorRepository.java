package br.com.empresa.helpdesk.admin.infra;

import br.com.empresa.helpdesk.admin.domain.Setor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetorRepository extends JpaRepository<Setor, Long> {
  List<Setor> findAllByOrderByNomeAsc();

  List<Setor> findByAtivoTrueOrderByNomeAsc();

  boolean existsByIdAndAtivoTrue(Long id);

  boolean existsByNomeIgnoreCase(String nome);
}
