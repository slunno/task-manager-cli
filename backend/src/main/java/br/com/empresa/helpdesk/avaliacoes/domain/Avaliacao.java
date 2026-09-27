package br.com.empresa.helpdesk.avaliacoes.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "avaliacoes")
public class Avaliacao {
  @Id
  @Column(name = "chamado_id")
  private Long chamadoId;

  @Column(nullable = false)
  private int nota;

  @Column(columnDefinition = "text")
  private String comentario;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm;

  protected Avaliacao() {}

  public Avaliacao(Long chamadoId, int nota, String comentario, Instant criadoEm) {
    this.chamadoId = chamadoId;
    this.nota = nota;
    this.comentario = comentario;
    this.criadoEm = criadoEm;
  }

  public Long getChamadoId() {
    return chamadoId;
  }

  public int getNota() {
    return nota;
  }

  public String getComentario() {
    return comentario;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
