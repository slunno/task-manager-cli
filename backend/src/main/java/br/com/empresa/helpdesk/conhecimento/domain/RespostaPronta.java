package br.com.empresa.helpdesk.conhecimento.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "respostas_prontas")
public class RespostaPronta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String titulo;

  @Column(nullable = false, columnDefinition = "text")
  private String texto;

  @Column(nullable = false)
  private boolean ativo;

  @Column(name = "autor_id", nullable = false, updatable = false)
  private Long autorId;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected RespostaPronta() {}

  public RespostaPronta(String titulo, String texto, Long autorId, Instant agora) {
    this.titulo = titulo;
    this.texto = texto;
    this.autorId = autorId;
    this.ativo = true;
    this.criadoEm = agora;
    this.atualizadoEm = agora;
  }

  public void editar(String titulo, String texto, boolean ativo, Instant agora) {
    this.titulo = titulo;
    this.texto = texto;
    this.ativo = ativo;
    this.atualizadoEm = agora;
  }

  public Long getId() {
    return id;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getTexto() {
    return texto;
  }

  public boolean isAtivo() {
    return ativo;
  }
}
