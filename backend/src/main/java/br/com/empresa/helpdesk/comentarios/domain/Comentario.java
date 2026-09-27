package br.com.empresa.helpdesk.comentarios.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "comentarios")
public class Comentario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "chamado_id", nullable = false, updatable = false)
  private Long chamadoId;

  @Column(name = "autor_id", nullable = false, updatable = false)
  private Long autorId;

  @Column(nullable = false, columnDefinition = "text", updatable = false)
  private String texto;

  @Column(nullable = false, updatable = false)
  private boolean interno;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  protected Comentario() {}

  public Comentario(Long chamadoId, Long autorId, String texto, boolean interno, Instant criadoEm) {
    this.chamadoId = chamadoId;
    this.autorId = autorId;
    this.texto = texto.trim();
    this.interno = interno;
    this.criadoEm = criadoEm;
  }

  public Long getId() {
    return id;
  }

  public Long getChamadoId() {
    return chamadoId;
  }

  public Long getAutorId() {
    return autorId;
  }

  public String getTexto() {
    return texto;
  }

  public boolean isInterno() {
    return interno;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
