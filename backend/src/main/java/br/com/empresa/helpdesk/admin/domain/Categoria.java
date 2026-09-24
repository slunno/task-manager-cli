package br.com.empresa.helpdesk.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categorias")
public class Categoria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 120)
  private String nome;

  @Column(nullable = false)
  private boolean ativa;

  protected Categoria() {}

  public Categoria(String nome) {
    this.nome = nome;
    this.ativa = true;
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public boolean isAtiva() {
    return ativa;
  }
}
