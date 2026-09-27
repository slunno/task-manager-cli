package br.com.empresa.helpdesk.conhecimento.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "artigos_conhecimento")
public class Artigo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String titulo;

  @Column(nullable = false, columnDefinition = "text")
  private String conteudo;

  @Column(name = "categoria_id")
  private Long categoriaId;

  @Column(nullable = false)
  private boolean publicado;

  @Column(name = "autor_id", nullable = false, updatable = false)
  private Long autorId;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected Artigo() {}

  public Artigo(
      String titulo,
      String conteudo,
      Long categoriaId,
      boolean publicado,
      Long autorId,
      Instant agora) {
    this.titulo = titulo;
    this.conteudo = conteudo;
    this.categoriaId = categoriaId;
    this.publicado = publicado;
    this.autorId = autorId;
    this.criadoEm = agora;
    this.atualizadoEm = agora;
  }

  public void editar(
      String titulo, String conteudo, Long categoriaId, boolean publicado, Instant agora) {
    this.titulo = titulo;
    this.conteudo = conteudo;
    this.categoriaId = categoriaId;
    this.publicado = publicado;
    this.atualizadoEm = agora;
  }

  public Long getId() {
    return id;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getConteudo() {
    return conteudo;
  }

  public Long getCategoriaId() {
    return categoriaId;
  }

  public boolean isPublicado() {
    return publicado;
  }

  public Long getAutorId() {
    return autorId;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }
}
