package br.com.empresa.helpdesk.sla.application;

import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.notificacoes.application.NotificacaoService;
import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertaSlaJob {
  private final ChamadoService chamados;
  private final UsuarioService usuarios;
  private final NotificacaoService notificacoes;
  private final Clock clock;

  public AlertaSlaJob(
      ChamadoService chamados,
      UsuarioService usuarios,
      NotificacaoService notificacoes,
      Clock clock) {
    this.chamados = chamados;
    this.usuarios = usuarios;
    this.notificacoes = notificacoes;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${helpdesk.sla.alert-poll-ms:60000}")
  @SchedulerLock(name = "alertas-sla", lockAtMostFor = "PT5M")
  public void executar() {
    Instant agora = Instant.now(clock);
    chamados
        .vencendoSla(agora, agora.plusSeconds(3600))
        .forEach(
            chamado ->
                usuarios
                    .buscarAtivoPorId(chamado.responsavelId())
                    .map(Usuario::getEmail)
                    .ifPresent(
                        email ->
                            notificacoes.enfileirarUnica(
                                "SLA_VENCENDO",
                                email,
                                chamado,
                                "SLA:"
                                    + chamado.id()
                                    + ":"
                                    + chamado.prazoResolucao().toEpochMilli())));
  }
}
