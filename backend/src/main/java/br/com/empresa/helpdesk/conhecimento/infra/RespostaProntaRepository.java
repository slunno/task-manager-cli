package br.com.empresa.helpdesk.conhecimento.infra;

import br.com.empresa.helpdesk.conhecimento.domain.RespostaPronta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespostaProntaRepository extends JpaRepository<RespostaPronta, Long> {
  List<RespostaPronta> findByAtivoTrueOrderByTituloAsc();
}
