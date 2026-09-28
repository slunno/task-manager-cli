package br.com.empresa.helpdesk.notificacoes.application;

import br.com.empresa.helpdesk.notificacoes.infra.NotificacaoOutboxRepository;
import jakarta.mail.MessagingException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

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
    var uri = java.net.URI.create(portal);
    if (!java.util.Set.of("http", "https").contains(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null)
      throw new IllegalArgumentException(
          "HELPDESK_PORTAL_URL deve ser um endereço HTTP(S) sem credenciais");
  }

  @Transactional
  public boolean enviar(Long id) {
    var item = repository.findById(id).orElseThrow();
    if (!"PENDENTE".equals(item.getStatus()) && !"FALHA".equals(item.getStatus())) return false;
    item.iniciarEnvio(Instant.now(clock));
    repository.saveAndFlush(item);
    try {
      var mensagem = mail.createMimeMessage();
      var helper = new MimeMessageHelper(mensagem, true, StandardCharsets.UTF_8.name());
      String numero = item.getPayload().get("numero");
      String titulo = item.getPayload().get("titulo");
      String status = statusLegivel(item.getPayload().getOrDefault("status", "ATUALIZADO"));
      String link =
          portal.replaceAll("/+$", "")
              + "/chamados/"
              + Long.parseLong(item.getPayload().get("chamadoId"));
      boolean resolucao = "RESOLUCAO".equals(item.getTipo());
      helper.setFrom(remetente);
      helper.setTo(item.getDestinatario());
      helper.setSubject(
          ("Helpdesk - " + numero + " - " + assunto(item.getTipo())).replaceAll("[\\r\\n]", " "));
      String texto =
          "Chamado "
              + numero
              + ": "
              + titulo
              + "\nStatus: "
              + status
              + "\n\nAcesse: "
              + link
              + (resolucao ? "\n\nAvalie o atendimento: " + link + "#avaliacao" : "");
      String html =
          "<!doctype html><html lang=\"pt-BR\"><body><h1>"
              + escape(assunto(item.getTipo()))
              + "</h1><p><strong>"
              + escape(numero)
              + "</strong></p><p>"
              + escape(titulo)
              + "</p><p>Status: "
              + escape(status)
              + "</p><p><a href=\""
              + escape(link)
              + "\">Abrir chamado</a></p>"
              + (resolucao
                  ? "<p><a href=\"" + escape(link + "#avaliacao") + "\">Avaliar atendimento</a></p>"
                  : "")
              + "</body></html>";
      helper.setText(texto, html);
      mail.send(mensagem);
      item.marcarEnviada(Instant.now(clock));
    } catch (org.springframework.mail.MailException | MessagingException ex) {
      item.marcarFalha(Instant.now(clock));
    }
    repository.save(item);
    return "ENVIADA".equals(item.getStatus());
  }

  private String escape(String valor) {
    return HtmlUtils.htmlEscape(valor, StandardCharsets.UTF_8.name());
  }

  private String statusLegivel(String valor) {
    return switch (valor) {
      case "ABERTO" -> "Aberto";
      case "EM_ATENDIMENTO" -> "Em atendimento";
      case "AGUARDANDO_USUARIO" -> "Aguardando usuário";
      case "RESOLVIDO" -> "Resolvido";
      case "FECHADO" -> "Fechado";
      default -> "Atualizado";
    };
  }

  private String assunto(String tipo) {
    return switch (tipo) {
      case "CHAMADO_CRIADO" -> "Novo chamado";
      case "ATRIBUICAO" -> "Chamado atribuído";
      case "COMENTARIO" -> "Novo comentário";
      case "RESOLUCAO" -> "Chamado resolvido";
      case "REABERTURA" -> "Chamado reaberto";
      case "SLA_VENCENDO" -> "SLA próximo do vencimento";
      case "SLA_RESOLUCAO" -> "Prazo de resolução próximo do vencimento";
      case "SLA_PRIMEIRA_RESPOSTA" -> "Prazo de primeira resposta próximo do vencimento";
      default -> "Atualização do chamado";
    };
  }
}
