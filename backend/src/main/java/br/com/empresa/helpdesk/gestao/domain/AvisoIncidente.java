package br.com.empresa.helpdesk.gestao.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "avisos_incidente")
public class AvisoIncidente {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 160)
  private String titulo;

  @Column(nullable = false, length = 1000)
  private String mensagem;

  @Column(nullable = false)
  private boolean ativo;

  @Column(name = "inicio_em", nullable = false)
  private Instant inicioEm;

  @Column(name = "fim_em")
  private Instant fimEm;

  @Column(name = "criado_por", nullable = false, updatable = false)
  private Long criadoPor;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected AvisoIncidente() {}

  public AvisoIncidente(
      String titulo,
      String mensagem,
      Instant inicioEm,
      Instant fimEm,
      Long criadoPor,
      Instant agora) {
    this.titulo = titulo;
    this.mensagem = mensagem;
    this.inicioEm = inicioEm;
    this.fimEm = fimEm;
    this.criadoPor = criadoPor;
    this.ativo = true;
    this.criadoEm = agora;
    this.atualizadoEm = agora;
  }

  public void editar(
      String titulo,
      String mensagem,
      Instant inicioEm,
      Instant fimEm,
      boolean ativo,
      Instant agora) {
    this.titulo = titulo;
    this.mensagem = mensagem;
    this.inicioEm = inicioEm;
    this.fimEm = fimEm;
    this.ativo = ativo;
    this.atualizadoEm = agora;
  }

  public Long getId() {
    return id;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getMensagem() {
    return mensagem;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public Instant getInicioEm() {
    return inicioEm;
  }

  public Instant getFimEm() {
    return fimEm;
  }
}
