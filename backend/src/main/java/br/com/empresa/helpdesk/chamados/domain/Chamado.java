package br.com.empresa.helpdesk.chamados.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "chamados")
public class Chamado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 24, updatable = false)
  private String numero;

  @Column(nullable = false, length = 200)
  private String titulo;

  @Column(nullable = false, columnDefinition = "text")
  private String descricao;

  @Column(name = "solicitante_id", nullable = false, updatable = false)
  private Long solicitanteId;

  @Column(name = "aberto_por_id", updatable = false)
  private Long abertoPorId;

  @Column(name = "categoria_id", nullable = false)
  private Long categoriaId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Prioridade prioridade;

  @Enumerated(EnumType.STRING)
  @Column(name = "prioridade_sugerida", length = 10)
  private Prioridade prioridadeSugerida;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private StatusChamado status;

  @Column(nullable = false, length = 20)
  private String canal;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  @Version private Long version;

  protected Chamado() {}

  private Chamado(
      String numero,
      String titulo,
      String descricao,
      Long solicitanteId,
      Long abertoPorId,
      Long categoriaId,
      Prioridade prioridadeSugerida,
      Instant agora) {
    this.numero = numero;
    this.titulo = titulo.trim();
    this.descricao = descricao.trim();
    this.solicitanteId = solicitanteId;
    this.abertoPorId = abertoPorId;
    this.categoriaId = categoriaId;
    this.prioridade = Prioridade.MEDIA;
    this.prioridadeSugerida = prioridadeSugerida;
    this.status = StatusChamado.ABERTO;
    this.canal = "PORTAL";
    this.criadoEm = agora;
    this.atualizadoEm = agora;
  }

  public static Chamado abrir(
      String numero,
      String titulo,
      String descricao,
      Long solicitanteId,
      Long abertoPorId,
      Long categoriaId,
      Prioridade prioridadeSugerida,
      Instant agora) {
    return new Chamado(
        numero,
        titulo,
        descricao,
        solicitanteId,
        abertoPorId,
        categoriaId,
        prioridadeSugerida,
        agora);
  }

  public Long getId() {
    return id;
  }

  public String getNumero() {
    return numero;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getDescricao() {
    return descricao;
  }

  public Long getSolicitanteId() {
    return solicitanteId;
  }

  public Long getAbertoPorId() {
    return abertoPorId;
  }

  public Long getCategoriaId() {
    return categoriaId;
  }

  public Prioridade getPrioridade() {
    return prioridade;
  }

  public Prioridade getPrioridadeSugerida() {
    return prioridadeSugerida;
  }

  public StatusChamado getStatus() {
    return status;
  }

  public String getCanal() {
    return canal;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }

  public Long getVersion() {
    return version;
  }
}
