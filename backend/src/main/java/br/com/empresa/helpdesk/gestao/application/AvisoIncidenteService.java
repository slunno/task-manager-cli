package br.com.empresa.helpdesk.gestao.application;

import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.gestao.domain.AvisoIncidente;
import br.com.empresa.helpdesk.gestao.infra.AvisoIncidenteRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvisoIncidenteService {
  private final AvisoIncidenteRepository repository;
  private final Clock clock;

  public AvisoIncidenteService(AvisoIncidenteRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<AvisoIncidente> vigentes() {
    return repository.vigentes(Instant.now(clock));
  }

  @Transactional(readOnly = true)
  public List<AvisoIncidente> todos() {
    return repository.findAllByOrderByCriadoEmDesc();
  }

  @Transactional
  public AvisoIncidente criar(
      String titulo, String mensagem, Instant inicioEm, Instant fimEm, Long autorId) {
    validar(inicioEm, fimEm);
    return repository.saveAndFlush(
        new AvisoIncidente(
            titulo.trim(), mensagem.trim(), inicioEm, fimEm, autorId, Instant.now(clock)));
  }

  @Transactional
  public AvisoIncidente editar(
      Long id, String titulo, String mensagem, Instant inicioEm, Instant fimEm, boolean ativo) {
    validar(inicioEm, fimEm);
    AvisoIncidente aviso =
        repository
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Aviso não encontrado"));
    aviso.editar(titulo.trim(), mensagem.trim(), inicioEm, fimEm, ativo, Instant.now(clock));
    return repository.saveAndFlush(aviso);
  }

  private void validar(Instant inicioEm, Instant fimEm) {
    if (inicioEm == null || (fimEm != null && !fimEm.isAfter(inicioEm)))
      throw new RequisicaoInvalidaException("Período do aviso inválido");
  }
}
