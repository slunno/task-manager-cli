package br.com.empresa.helpdesk.admin.application;

import br.com.empresa.helpdesk.admin.domain.Setor;
import br.com.empresa.helpdesk.admin.infra.SetorRepository;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetorService {
  private final SetorRepository repository;
  private final Clock clock;

  public SetorService(SetorRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<Setor> todos() {
    return repository.findAllByOrderByNomeAsc();
  }

  @Transactional(readOnly = true)
  public List<Setor> ativos() {
    return repository.findByAtivoTrueOrderByNomeAsc();
  }

  @Transactional(readOnly = true)
  public void exigirAtivo(Long id) {
    if (!repository.existsByIdAndAtivoTrue(id))
      throw new RecursoNaoEncontradoException("Setor não encontrado ou inativo");
  }

  @Transactional
  public Setor criar(String nome) {
    String limpo = nome.trim();
    if (repository.existsByNomeIgnoreCase(limpo))
      throw new RequisicaoInvalidaException("Setor já cadastrado");
    return repository.saveAndFlush(new Setor(limpo, Instant.now(clock)));
  }

  @Transactional
  public Setor editar(Long id, String nome, boolean ativo) {
    Setor setor =
        repository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Setor não encontrado"));
    String limpo = nome.trim();
    if (!setor.getNome().equalsIgnoreCase(limpo) && repository.existsByNomeIgnoreCase(limpo))
      throw new RequisicaoInvalidaException("Setor já cadastrado");
    setor.atualizar(limpo, ativo, Instant.now(clock));
    return repository.saveAndFlush(setor);
  }
}
