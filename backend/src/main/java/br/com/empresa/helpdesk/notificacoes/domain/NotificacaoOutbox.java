package br.com.empresa.helpdesk.notificacoes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notificacoes_outbox")
public class NotificacaoOutbox {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 60)
  private String tipo;

  @Column(nullable = false, length = 254)
  private String destinatario;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, String> payload;

  @Column(name = "dedup_key", length = 180)
  private String dedupKey;

  @Column(nullable = false, length = 16)
  private String status;

  @Column(nullable = false)
  private int tentativas;

  @Column(name = "proxima_tentativa_em", nullable = false)
  private Instant proximaTentativaEm;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected NotificacaoOutbox() {}

  public NotificacaoOutbox(
      String tipo, String destinatario, Map<String, String> payload, Instant agora) {
    this.tipo = tipo;
    this.destinatario = destinatario;
    this.payload = Map.copyOf(payload);
    this.status = "PENDENTE";
    this.proximaTentativaEm = agora;
    this.criadoEm = agora;
    this.atualizadoEm = agora;
  }

  public void definirDedupKey(String chave) {
    this.dedupKey = chave;
  }

  public Long getId() {
    return id;
  }

  public String getTipo() {
    return tipo;
  }

  public String getDedupKey() {
    return dedupKey;
  }

  public String getDestinatario() {
    return destinatario;
  }

  public Map<String, String> getPayload() {
    return payload;
  }

  public String getStatus() {
    return status;
  }

  public int getTentativas() {
    return tentativas;
  }

  public Instant getProximaTentativaEm() {
    return proximaTentativaEm;
  }

  public void iniciarEnvio(Instant agora) {
    status = "ENVIANDO";
    atualizadoEm = agora;
  }

  public void marcarEnviada(Instant agora) {
    status = "ENVIADA";
    tentativas++;
    atualizadoEm = agora;
  }

  public void marcarFalha(Instant agora) {
    status = "FALHA";
    tentativas++;
    long segundos = Math.min(3600, 30L << Math.min(tentativas - 1, 7));
    proximaTentativaEm = agora.plusSeconds(segundos);
    atualizadoEm = agora;
  }

  public void recuperarEnvioInterrompido(Instant agora) {
    status = "FALHA";
    proximaTentativaEm = agora;
    atualizadoEm = agora;
  }
}
