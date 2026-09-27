package br.com.empresa.helpdesk.notificacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.empresa.helpdesk.notificacoes.application.EnvioNotificacaoService;
import br.com.empresa.helpdesk.notificacoes.domain.NotificacaoOutbox;
import br.com.empresa.helpdesk.notificacoes.infra.NotificacaoOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

class EnvioNotificacaoServiceTest {
  private final Instant agora = Instant.parse("2026-09-27T12:00:00Z");
  private final Clock clock = Clock.fixed(agora, ZoneOffset.UTC);

  @Test
  void falhaSmtpMantemMensagemPendenteComBackoff() {
    var repository = mock(NotificacaoOutboxRepository.class);
    var mail = mock(JavaMailSender.class);
    var item =
        new NotificacaoOutbox(
            "RESOLUCAO",
            "maria@example.com",
            Map.of("numero", "CH-2026-000001", "titulo", "Problema", "chamadoId", "1"),
            agora);
    when(repository.findById(1L)).thenReturn(Optional.of(item));
    doThrow(new MailSendException("indisponível"))
        .when(mail)
        .send(any(org.springframework.mail.SimpleMailMessage.class));
    var servico =
        new EnvioNotificacaoService(
            repository, mail, clock, "helpdesk@example.com", "http://portal");
    assertThat(servico.enviar(1L)).isFalse();
    assertThat(item.getStatus()).isEqualTo("FALHA");
    assertThat(item.getTentativas()).isEqualTo(1);
    assertThat(item.getProximaTentativaEm()).isEqualTo(agora.plusSeconds(30));
  }
}
