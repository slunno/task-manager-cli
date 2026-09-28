package br.com.empresa.helpdesk.notificacoes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.empresa.helpdesk.notificacoes.application.EnvioNotificacaoService;
import br.com.empresa.helpdesk.notificacoes.domain.NotificacaoOutbox;
import br.com.empresa.helpdesk.notificacoes.infra.NotificacaoOutboxRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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
    when(mail.createMimeMessage())
        .thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    doThrow(new MailSendException("indisponível")).when(mail).send(any(MimeMessage.class));
    var servico =
        new EnvioNotificacaoService(
            repository, mail, clock, "helpdesk@example.com", "http://portal");
    assertThat(servico.enviar(1L)).isFalse();
    assertThat(item.getStatus()).isEqualTo("FALHA");
    assertThat(item.getTentativas()).isEqualTo(1);
    assertThat(item.getProximaTentativaEm()).isEqualTo(agora.plusSeconds(30));
  }

  @Test
  void emailContemHtmlEscapadoTextoAlternativoELinkDeAvaliacao() throws Exception {
    var repository = mock(NotificacaoOutboxRepository.class);
    var mail = mock(JavaMailSender.class);
    var item =
        new NotificacaoOutbox(
            "RESOLUCAO",
            "maria@example.com",
            Map.of(
                "numero",
                "CH-2026-000001",
                "titulo",
                "<script>teste</script>",
                "chamadoId",
                "1",
                "status",
                "RESOLVIDO"),
            agora);
    when(repository.findById(1L)).thenReturn(Optional.of(item));
    MimeMessage mensagem = new MimeMessage(Session.getInstance(new Properties()));
    when(mail.createMimeMessage()).thenReturn(mensagem);
    var servico =
        new EnvioNotificacaoService(
            repository, mail, clock, "helpdesk@example.com", "http://portal");
    assertThat(servico.enviar(1L)).isTrue();
    mensagem.saveChanges();
    var partes = textos(mensagem);
    assertThat(partes.get("text/plain"))
        .contains("CH-2026-000001", "Status: Resolvido", "http://portal/chamados/1#avaliacao");
    assertThat(partes.get("text/html"))
        .contains("&lt;script&gt;teste&lt;/script&gt;", "#avaliacao")
        .doesNotContain("<script>", "Descrição", "solucao");
    assertThat(mensagem.getAllRecipients()).hasSize(1);
  }

  private Map<String, String> textos(jakarta.mail.Part parte) throws Exception {
    Map<String, String> resultado = new java.util.HashMap<>();
    if (parte.isMimeType("text/plain")) resultado.put("text/plain", (String) parte.getContent());
    else if (parte.isMimeType("text/html")) resultado.put("text/html", (String) parte.getContent());
    else if (parte.getContent() instanceof jakarta.mail.Multipart multi)
      for (int i = 0; i < multi.getCount(); i++) resultado.putAll(textos(multi.getBodyPart(i)));
    return resultado;
  }
}
