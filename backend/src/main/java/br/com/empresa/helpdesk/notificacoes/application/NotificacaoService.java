package br.com.empresa.helpdesk.notificacoes.application;

import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.chamados.domain.ChamadoAlteradoEvent;
import br.com.empresa.helpdesk.chamados.domain.ChamadoReabertoEvent;
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
  private final DestinatariosTi ti;

  public NotificacaoService(
      NotificacaoOutboxRepository repository,
      UsuarioService usuarios,
      ChamadoService chamados,
      Clock clock,
      DestinatariosTi ti) {
    this.repository = repository;
    this.usuarios = usuarios;
    this.chamados = chamados;
    this.clock = clock;
    this.ti = ti;
  }

  @EventListener
  public void criado(ChamadoCriadoEvent evento) {
    var chamado = chamados.resumoParaNotificacao(evento.chamadoId());
    ti.emails().forEach(email -> enfileirar("CHAMADO_CRIADO", email, chamado));
  }

  @EventListener
  public void alterado(ChamadoAlteradoEvent evento) {
    var chamado = chamados.resumoParaNotificacao(evento.chamadoId());
    if ("responsavel".equals(evento.campo()) && evento.valorNovo() != null)
      destinatarioAtivo(Long.valueOf(evento.valorNovo()), "ATRIBUICAO", chamado);
    if ("status".equals(evento.campo())
        && !java.util.Objects.equals(evento.usuarioId(), chamado.solicitanteId()))
      destinatarioAtivo(
          chamado.solicitanteId(),
          "RESOLVIDO".equals(evento.valorNovo()) ? "RESOLUCAO" : "STATUS",
          chamado);
  }

  @EventListener
  public void reaberto(ChamadoReabertoEvent evento) {
    var chamado = chamados.resumoParaNotificacao(evento.chamadoId());
    ti.emails().forEach(email -> enfileirar("REABERTURA", email, chamado));
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
    else ti.emails().forEach(email -> enfileirar("COMENTARIO", email, chamado));
  }

  public void alertarPrazo(
      ChamadoService.ResumoNotificacao chamado, String prazo, Instant instante) {
    var responsavel =
        chamado.responsavelId() == null
            ? java.util.Optional.<Usuario>empty()
            : usuarios.buscarAtivoPorId(chamado.responsavelId());
    var emails =
        responsavel.map(pessoa -> java.util.List.of(pessoa.getEmail())).orElseGet(ti::emails);
    for (String email : emails) {
      String destinatario =
          java.util
              .UUID
              .nameUUIDFromBytes(email.getBytes(java.nio.charset.StandardCharsets.UTF_8))
              .toString();
      enfileirarUnica(
          "SLA_" + prazo,
          email,
          chamado,
          "SLA:" + chamado.id() + ":" + prazo + ":" + instante.toEpochMilli() + ":" + destinatario);
    }
  }

  private void destinatarioAtivo(
      Long usuarioId, String tipo, ChamadoService.ResumoNotificacao chamado) {
    usuarios
        .buscarAtivoPorId(usuarioId)
        .map(Usuario::getEmail)
        .ifPresent(email -> enfileirar(tipo, email, chamado));
  }

  public void enfileirarUnica(
      String tipo, String email, ChamadoService.ResumoNotificacao chamado, String chave) {
    if (repository.existsByDedupKey(chave)) return;
    var item = criarItem(tipo, email, chamado);
    item.definirDedupKey(chave);
    repository.save(item);
  }

  public void enfileirar(String tipo, String email, ChamadoService.ResumoNotificacao chamado) {
    repository.save(criarItem(tipo, email, chamado));
  }

  private NotificacaoOutbox criarItem(
      String tipo, String email, ChamadoService.ResumoNotificacao chamado) {
    return new NotificacaoOutbox(
        tipo,
        email,
        Map.of(
            "numero",
            chamado.numero(),
            "titulo",
            tipo.startsWith("SLA_")
                ? "Prazo de atendimento próximo do vencimento"
                : chamado.titulo(),
            "status",
            chamado.status().name(),
            "chamadoId",
            chamado.id().toString()),
        Instant.now(clock));
  }
}
