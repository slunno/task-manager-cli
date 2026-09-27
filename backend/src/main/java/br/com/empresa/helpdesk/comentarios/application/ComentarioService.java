package br.com.empresa.helpdesk.comentarios.application;

import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.comentarios.api.ComentarioResponse;
import br.com.empresa.helpdesk.comentarios.api.CriarComentarioRequest;
import br.com.empresa.helpdesk.comentarios.domain.Comentario;
import br.com.empresa.helpdesk.comentarios.infra.ComentarioRepository;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.notificacoes.application.ComentarioCriadoEvent;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComentarioService {
  private final ComentarioRepository repository;
  private final ChamadoService chamados;
  private final Clock clock;
  private final org.springframework.context.ApplicationEventPublisher eventos;

  public ComentarioService(
      ComentarioRepository repository,
      ChamadoService chamados,
      Clock clock,
      org.springframework.context.ApplicationEventPublisher eventos) {
    this.repository = repository;
    this.chamados = chamados;
    this.clock = clock;
    this.eventos = eventos;
  }

  @Transactional
  public ComentarioResponse criar(Long chamadoId, CriarComentarioRequest dados, Usuario ator) {
    chamados.exigirAcesso(chamadoId, ator);
    if (dados.interno() && ator.getPerfil() == Perfil.FUNCIONARIO) {
      throw new AccessDeniedException("Nota interna exclusiva da TI");
    }
    Instant agora = Instant.now(clock);
    chamados.registrarComentario(chamadoId, ator, dados.interno(), agora);
    var comentario =
        repository.saveAndFlush(
            new Comentario(chamadoId, ator.getId(), dados.texto(), dados.interno(), agora));
    eventos.publishEvent(new ComentarioCriadoEvent(chamadoId, ator.getId(), dados.interno()));
    return ComentarioResponse.de(comentario);
  }

  @Transactional(readOnly = true)
  public boolean visibilidadeDoComentario(Long comentarioId, Long chamadoId) {
    return repository
        .findByIdAndChamadoId(comentarioId, chamadoId)
        .orElseThrow(() -> new RecursoNaoEncontradoException("Comentário não encontrado"))
        .isInterno();
  }

  @Transactional(readOnly = true)
  public PaginaResponse<ComentarioResponse> listar(
      Long chamadoId, Usuario ator, int page, int size) {
    chamados.exigirAcesso(chamadoId, ator);
    if (page < 0 || size < 1 || size > 100) {
      throw new RequisicaoInvalidaException("Paginação inválida: page >= 0 e size entre 1 e 100");
    }
    var ordem = PageRequest.of(page, size, Sort.by("criadoEm").ascending().and(Sort.by("id")));
    var resultado =
        ator.getPerfil() == Perfil.FUNCIONARIO
            ? repository.findByChamadoIdAndInternoFalse(chamadoId, ordem)
            : repository.findByChamadoId(chamadoId, ordem);
    return PaginaResponse.de(resultado.map(ComentarioResponse::de));
  }
}
