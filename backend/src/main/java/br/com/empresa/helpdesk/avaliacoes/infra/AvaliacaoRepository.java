package br.com.empresa.helpdesk.avaliacoes.infra;

import br.com.empresa.helpdesk.avaliacoes.domain.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {}
