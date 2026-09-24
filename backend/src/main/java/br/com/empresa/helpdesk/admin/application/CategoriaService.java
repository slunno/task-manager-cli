package br.com.empresa.helpdesk.admin.application;

import br.com.empresa.helpdesk.admin.domain.Categoria;
import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {
  private final CategoriaRepository repository;

  public CategoriaService(CategoriaRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Categoria> listarAtivas() {
    return repository.findByAtivaTrueOrderByNomeAsc();
  }

  @Transactional(readOnly = true)
  public void exigirAtiva(Long id) {
    if (!repository.existsByIdAndAtivaTrue(id)) {
      throw new RecursoNaoEncontradoException("Categoria não encontrada");
    }
  }
}
