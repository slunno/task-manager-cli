package br.com.empresa.helpdesk.notificacoes.application;

import br.com.empresa.helpdesk.notificacoes.infra.NotificacaoOutboxRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvioNotificacaoService {
  private final NotificacaoOutboxRepository repository;
  private final JavaMailSender mail;
  private final Clock clock;
  private final String remetente;
  private final String portal;

  public EnvioNotificacaoService(
      NotificacaoOutboxRepository repository,
      JavaMailSender mail,
      Clock clock,
      @Value("${helpdesk.mail.from:helpdesk@localhost}") String remetente,
      @Value("${helpdesk.portal.url:http://127.0.0.1:5173}") String portal) {
    this.repository = repository;
    this.mail = mail;
    this.clock = clock;
    this.remetente = remetente;
    this.portal = portal;
  }

  @Transactional
  public boolean enviar(Long id) {
    var item = repository.findById(id).orElseThrow();
    if (!"PENDENTE".equals(item.getStatus()) && !"FALHA".equals(item.getStatus())) return false;
    item.iniciarEnvio(Instant.now(clock));
    repository.saveAndFlush(item);
    try {
      SimpleMailMessage mensagem = new SimpleMailMessage();
      mensagem.setFrom(remetente);
      mensagem.setTo(item.getDestinatario());
      mensagem.setSubject(
          "Helpdesk - " + item.getPayload().get("numero") + " - " + assunto(item.getTipo()));
      mensagem.setText(
          "Chamado "
              + item.getPayload().get("numero")
              + ": "
              + item.getPayload().get("titulo")
              + "\n\nAcesse: "
              + portal
              + "/chamados/"
              + item.getPayload().get("chamadoId")
              + ("RESOLUCAO".equals(item.getTipo()) ? "\n\nAvalie o atendimento no portal." : ""));
      mail.send(mensagem);
      item.marcarEnviada(Instant.now(clock));
    } catch (org.springframework.mail.MailException ex) {
      item.marcarFalha(Instant.now(clock));
    }
    repository.save(item);
    return "ENVIADA".equals(item.getStatus());
  }

  private String assunto(String tipo) {
    return switch (tipo) {
      case "CHAMADO_CRIADO" -> "Novo chamado";
      case "ATRIBUICAO" -> "Chamado atribuído";
      case "COMENTARIO" -> "Novo comentário";
      case "RESOLUCAO" -> "Chamado resolvido";
      case "REABERTURA" -> "Chamado reaberto";
      case "SLA_VENCENDO" -> "SLA próximo do vencimento";
      default -> "Atualização do chamado";
    };
  }
}
