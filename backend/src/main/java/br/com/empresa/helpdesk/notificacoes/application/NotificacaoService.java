package br.com.empresa.helpdesk.notificacoes.application;

import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.chamados.domain.ChamadoAlteradoEvent;
import br.com.empresa.helpdesk.notificacoes.domain.NotificacaoOutbox;
import br.com.empresa.helpdesk.notificacoes.infra.NotificacaoOutboxRepository;
import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class NotificacaoService {
  private final NotificacaoOutboxRepository repository;
  private final UsuarioService usuarios;
  private final ChamadoService chamados;
  private final Clock clock;

  public NotificacaoService(
      NotificacaoOutboxRepository repository,
      UsuarioService usuarios,
      ChamadoService chamados,
      Clock clock) {
    this.repository = repository;
    this.usuarios = usuarios;
    this.chamados = chamados;
    this.clock = clock;
  }

  @EventListener
  public void criado(ChamadoCriadoEvent evento) {
    var chamado = chamados.resumoParaNotificacao(evento.chamadoId());
    usuarios.emailsTiAtivos().forEach(email -> enfileirar("CHAMADO_CRIADO", email, chamado));
  }

  @EventListener
  public void alterado(ChamadoAlteradoEvent evento) {
    var chamado = chamados.resumoParaNotificacao(evento.chamadoId());
    if ("responsavel".equals(evento.campo()) && evento.valorNovo() != null)
      destinatarioAtivo(Long.valueOf(evento.valorNovo()), "ATRIBUICAO", chamado);
    if ("status".equals(evento.campo()) && !evento.usuarioId().equals(chamado.solicitanteId()))
      destinatarioAtivo(
          chamado.solicitanteId(),
          "RESOLVIDO".equals(evento.valorNovo()) ? "RESOLUCAO" : "STATUS",
          chamado);
  }

  @EventListener
  public void comentario(ComentarioCriadoEvent evento) {
    if (evento.interno()) return;
    var chamado = chamados.resumoParaNotificacao(evento.chamadoId());
    Long destinatarioId =
        evento.autorId().equals(chamado.solicitanteId())
            ? chamado.responsavelId()
            : chamado.solicitanteId();
    if (destinatarioId != null) destinatarioAtivo(destinatarioId, "COMENTARIO", chamado);
  }

  private void destinatarioAtivo(
      Long usuarioId, String tipo, ChamadoService.ResumoNotificacao chamado) {
    usuarios
        .buscarAtivoPorId(usuarioId)
        .map(Usuario::getEmail)
        .ifPresent(email -> enfileirar(tipo, email, chamado));
  }

  public void enfileirar(String tipo, String email, ChamadoService.ResumoNotificacao chamado) {
    repository.save(
        new NotificacaoOutbox(
            tipo,
            email,
            Map.of(
                "numero",
                chamado.numero(),
                "titulo",
                chamado.titulo(),
                "chamadoId",
                chamado.id().toString()),
            Instant.now(clock)));
  }
}
