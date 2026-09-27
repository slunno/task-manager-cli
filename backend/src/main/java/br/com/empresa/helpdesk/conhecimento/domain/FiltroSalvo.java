package br.com.empresa.helpdesk.conhecimento.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "filtros_salvos",
    uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "nome"}))
public class FiltroSalvo {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "usuario_id", nullable = false, updatable = false)
  private Long usuarioId;

  @Column(nullable = false, length = 80)
  private String nome;

  @Column(nullable = false, length = 1000)
  private String parametros;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  protected FiltroSalvo() {}

  public FiltroSalvo(Long usuarioId, String nome, String parametros, Instant agora) {
    this.usuarioId = usuarioId;
    this.nome = nome;
    this.parametros = parametros;
    this.criadoEm = agora;
  }

  public Long getId() {
    return id;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }

  public String getNome() {
    return nome;
  }

  public String getParametros() {
    return parametros;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }
}
