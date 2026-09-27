package br.com.empresa.helpdesk.anexos.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "anexos")
public class Anexo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "chamado_id", nullable = false, updatable = false)
  private Long chamadoId;

  @Column(name = "comentario_id", updatable = false)
  private Long comentarioId;

  @Column(name = "nome_original", nullable = false, length = 255, updatable = false)
  private String nomeOriginal;

  @Column(name = "chave_storage", nullable = false, length = 255, unique = true, updatable = false)
  private String chaveStorage;

  @Column(name = "tipo_mime", nullable = false, length = 120, updatable = false)
  private String tipoMime;

  @Column(nullable = false, updatable = false)
  private long tamanho;

  @Column(name = "criado_por", nullable = false, updatable = false)
  private Long criadoPor;

  @Column(nullable = false, updatable = false)
  private boolean interno;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  protected Anexo() {}

  public Anexo(
      Long chamadoId,
      Long comentarioId,
      String nomeOriginal,
      String chaveStorage,
      String tipoMime,
      long tamanho,
      Long criadoPor,
      boolean interno,
      Instant criadoEm) {
    this.chamadoId = chamadoId;
    this.comentarioId = comentarioId;
    this.nomeOriginal = nomeOriginal;
    this.chaveStorage = chaveStorage;
    this.tipoMime = tipoMime;
    this.tamanho = tamanho;
    this.criadoPor = criadoPor;
    this.interno = interno;
    this.criadoEm = criadoEm;
  }

  public Long getId() {
    return id;
  }

  public Long getChamadoId() {
    return chamadoId;
  }

  public Long getComentarioId() {
    return comentarioId;
  }

  public String getNomeOriginal() {
    return nomeOriginal;
  }

  public String getChaveStorage() {
    return chaveStorage;
  }

  public String getTipoMime() {
    return tipoMime;
  }

  public long getTamanho() {
    return tamanho;
  }

  public Long getCriadoPor() {
    return criadoPor;
  }

  public boolean isInterno() {
    return interno;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
