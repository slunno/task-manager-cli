package br.com.empresa.helpdesk.admin.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "setores")
public class Setor {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false)
  private boolean ativo;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected Setor() {}

  public Setor(String nome, Instant agora) {
    this.nome = nome;
    this.ativo = true;
    this.criadoEm = agora;
    this.atualizadoEm = agora;
  }

  public void atualizar(String nome, boolean ativo, Instant agora) {
    this.nome = nome;
    this.ativo = ativo;
    this.atualizadoEm = agora;
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public boolean isAtivo() {
    return ativo;
  }
}
