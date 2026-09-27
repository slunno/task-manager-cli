package br.com.empresa.helpdesk.avaliacoes.application;

import br.com.empresa.helpdesk.avaliacoes.domain.Avaliacao;
import br.com.empresa.helpdesk.avaliacoes.infra.AvaliacaoRepository;
import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.chamados.domain.ChamadoReabertoEvent;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import br.com.empresa.helpdesk.compartilhado.erros.ConflitoChamadoException;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvaliacaoService {
  private final AvaliacaoRepository repository;
  private final ChamadoService chamados;
  private final Clock clock;

  public AvaliacaoService(AvaliacaoRepository repository, ChamadoService chamados, Clock clock) {
    this.repository = repository;
    this.chamados = chamados;
    this.clock = clock;
  }

  @Transactional
  public Avaliacao avaliar(Long chamadoId, int nota, String comentario, Usuario ator) {
    var chamado = chamados.exigirAcesso(chamadoId, ator);
    if (!chamado.getSolicitanteId().equals(ator.getId()))
      throw new AccessDeniedException("Somente o solicitante pode avaliar");
    if (chamado.getStatus() != StatusChamado.RESOLVIDO
        && chamado.getStatus() != StatusChamado.FECHADO)
      throw new ConflitoChamadoException("Avaliação permitida após a resolução");
    if (nota < 1 || nota > 5 || (comentario != null && comentario.length() > 1000))
      throw new RequisicaoInvalidaException("Nota ou comentário inválido");
    if (repository.existsById(chamadoId)) throw new ConflitoChamadoException("Chamado já avaliado");
    String texto = comentario == null || comentario.isBlank() ? null : comentario.trim();
    return repository.saveAndFlush(new Avaliacao(chamadoId, nota, texto, Instant.now(clock)));
  }

  @Transactional(readOnly = true)
  public Avaliacao buscar(Long chamadoId, Usuario ator) {
    chamados.exigirAcesso(chamadoId, ator);
    return repository
        .findById(chamadoId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação não encontrada"));
  }

  @EventListener
  public void reaberto(ChamadoReabertoEvent evento) {
    repository.deleteById(evento.chamadoId());
  }
}
