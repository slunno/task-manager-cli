package br.com.empresa.helpdesk.compartilhado.retencao;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "helpdesk.retencao.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RetencaoJob {
  private final RetencaoService service;

  public RetencaoJob(RetencaoService service) {
    this.service = service;
  }

  @Scheduled(cron = "${helpdesk.retencao.cron:0 0 3 * * *}", zone = "America/Sao_Paulo")
  @SchedulerLock(name = "retencao-lgpd", lockAtMostFor = "PT30M")
  public void executar() {
    for (int lote = 0; lote < 10; lote++) {
      if (service.anonimizarLote() < 100) break;
    }
    for (int lote = 0; lote < 10; lote++) {
      if (service.limparObjetosPendentes() < 100) break;
    }
  }
}
