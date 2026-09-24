package br.com.empresa.helpdesk.usuarios.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "usuarios")
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 180)
  private String nome;

  @Column(nullable = false, length = 254)
  private String email;

  @Column(name = "setor_id", insertable = false, updatable = false)
  private Long setorId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Perfil perfil;

  @Column(nullable = false)
  private boolean ativo;

  @Column(name = "ultimo_login_em")
  private Instant ultimoLoginEm;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected Usuario() {}

  private Usuario(String nome, String email) {
    this.nome = nome;
    this.email = email;
    this.perfil = Perfil.FUNCIONARIO;
    this.ativo = true;
  }

  public static Usuario novoFuncionario(String nome, String email) {
    return new Usuario(nome, email);
  }

  public void registrarLogin(Instant instante) {
    this.ultimoLoginEm = instante;
  }

  public void alterarPerfil(Perfil novoPerfil) {
    this.perfil = novoPerfil;
  }

  public void desativar() {
    this.ativo = false;
  }

  @PrePersist
  void aoCriar() {
    Instant agora = Instant.now();
    this.criadoEm = agora;
    this.atualizadoEm = agora;
  }

  @PreUpdate
  void aoAtualizar() {
    this.atualizadoEm = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getEmail() {
    return email;
  }

  public Long getSetorId() {
    return setorId;
  }

  public Perfil getPerfil() {
    return perfil;
  }

  public boolean isAtivo() {
    return ativo;
  }

  public Instant getUltimoLoginEm() {
    return ultimoLoginEm;
  }
}
