package br.com.empresa.helpdesk.admin.application;

import br.com.empresa.helpdesk.admin.domain.Categoria;
import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaAdminService {
  private final CategoriaRepository repository;

  public CategoriaAdminService(CategoriaRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public List<Categoria> listar() {
    return repository.findAll(Sort.by("nome"));
  }

  @Transactional
  public Categoria criar(String nome) {
    String normalizado = validar(nome);
    if (repository.existsByNomeIgnoreCase(normalizado))
      throw new RequisicaoInvalidaException("Categoria já cadastrada");
    return repository.save(new Categoria(normalizado));
  }

  @Transactional
  public Categoria atualizar(Long id, String nome, boolean ativa) {
    Categoria categoria =
        repository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada"));
    String normalizado = validar(nome);
    if (!categoria.getNome().equalsIgnoreCase(normalizado)
        && repository.existsByNomeIgnoreCase(normalizado))
      throw new RequisicaoInvalidaException("Categoria já cadastrada");
    categoria.atualizar(normalizado, ativa);
    return repository.save(categoria);
  }

  private String validar(String nome) {
    if (nome == null || nome.isBlank() || nome.trim().length() > 120)
      throw new RequisicaoInvalidaException("Nome da categoria deve ter entre 1 e 120 caracteres");
    return nome.trim();
  }
}
