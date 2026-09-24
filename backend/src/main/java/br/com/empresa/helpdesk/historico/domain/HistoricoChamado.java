package br.com.empresa.helpdesk.historico.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "historico_chamado")
public class HistoricoChamado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "chamado_id", nullable = false, updatable = false)
  private Long chamadoId;

  @Column(name = "usuario_id", nullable = false, updatable = false)
  private Long usuarioId;

  @Column(nullable = false, length = 60, updatable = false)
  private String campo;

  @Column(name = "valor_anterior", columnDefinition = "text", updatable = false)
  private String valorAnterior;

  @Column(name = "valor_novo", columnDefinition = "text", updatable = false)
  private String valorNovo;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  protected HistoricoChamado() {}

  public HistoricoChamado(
      Long chamadoId,
      Long usuarioId,
      String campo,
      String valorAnterior,
      String valorNovo,
      Instant criadoEm) {
    this.chamadoId = chamadoId;
    this.usuarioId = usuarioId;
    this.campo = campo;
    this.valorAnterior = valorAnterior;
    this.valorNovo = valorNovo;
    this.criadoEm = criadoEm;
  }

  public Long getId() {
    return id;
  }

  public Long getChamadoId() {
    return chamadoId;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }

  public String getCampo() {
    return campo;
  }

  public String getValorAnterior() {
    return valorAnterior;
  }

  public String getValorNovo() {
    return valorNovo;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
