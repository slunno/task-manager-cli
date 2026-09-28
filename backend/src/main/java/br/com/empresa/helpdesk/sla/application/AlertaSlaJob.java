package br.com.empresa.helpdesk.sla.application;

import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.notificacoes.application.NotificacaoService;
import java.time.Clock;
import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AlertaSlaJob {
  private final ChamadoService chamados;
  private final NotificacaoService notificacoes;
  private final Clock clock;

  public AlertaSlaJob(ChamadoService chamados, NotificacaoService notificacoes, Clock clock) {
    this.chamados = chamados;
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
            chamado -> notificacoes.alertarPrazo(chamado, "RESOLUCAO", chamado.prazoResolucao()));
    chamados
        .vencendoPrimeiraResposta(agora, agora.plusSeconds(3600))
        .forEach(
            chamado ->
                notificacoes.alertarPrazo(
                    chamado, "PRIMEIRA_RESPOSTA", chamado.prazoPrimeiraResposta()));
  }
}
