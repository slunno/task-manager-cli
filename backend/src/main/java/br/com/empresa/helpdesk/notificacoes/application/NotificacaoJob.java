package br.com.empresa.helpdesk.notificacoes.application;

import br.com.empresa.helpdesk.notificacoes.infra.NotificacaoOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificacaoJob {
  private final NotificacaoOutboxRepository repository;
  private final EnvioNotificacaoService envio;
  private final Clock clock;

  public NotificacaoJob(
      NotificacaoOutboxRepository repository, EnvioNotificacaoService envio, Clock clock) {
    this.repository = repository;
    this.envio = envio;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${helpdesk.mail.poll-ms:30000}")
  @SchedulerLock(name = "outbox-notificacoes", lockAtMostFor = "PT5M")
  public void executar() {
    recuperarEnviosInterrompidos();
    repository
        .pendentes(Instant.now(clock), PageRequest.of(0, 50))
        .forEach(item -> envio.enviar(item.getId()));
  }

  @Transactional
  public void recuperarEnviosInterrompidos() {
    Instant agora = Instant.now(clock);
    repository
        .enviosInterrompidos(agora.minusSeconds(300))
        .forEach(item -> item.recuperarEnvioInterrompido(agora));
  }
}
