package br.com.empresa.helpdesk.chamados.application;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FechamentoAutomaticoJob {
  private final ChamadoService chamados;

  public FechamentoAutomaticoJob(ChamadoService chamados) {
    this.chamados = chamados;
  }

  @Scheduled(fixedDelayString = "${helpdesk.conclusao.poll-ms:3600000}")
  @SchedulerLock(name = "fechamento-automatico", lockAtMostFor = "PT10M")
  public void executar() {
    chamados.fecharAntigos();
  }
}
