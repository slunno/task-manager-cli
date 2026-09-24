package br.com.empresa.helpdesk.chamados.domain;

import br.com.empresa.helpdesk.compartilhado.erros.ConflitoChamadoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "chamados")
public class Chamado {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 24, updatable = false)
  private String numero;

  @Column(nullable = false, length = 200)
  private String titulo;

  @Column(nullable = false, columnDefinition = "text")
  private String descricao;

  @Column(name = "solicitante_id", nullable = false, updatable = false)
  private Long solicitanteId;

  @Column(name = "aberto_por_id", updatable = false)
  private Long abertoPorId;

  @Column(name = "categoria_id", nullable = false)
  private Long categoriaId;

  @Column(name = "responsavel_id")
  private Long responsavelId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Prioridade prioridade;

  @Enumerated(EnumType.STRING)
  @Column(name = "prioridade_sugerida", length = 10)
  private Prioridade prioridadeSugerida;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  private StatusChamado status;

  @Column(nullable = false, length = 20)
  private String canal;

  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  @Column(name = "resolvido_em")
  private Instant resolvidoEm;

  @Column(columnDefinition = "text")
  private String solucao;

  @Column(name = "prazo_resolucao")
  private Instant prazoResolucao;

  @Version private Long version;

  protected Chamado() {}

  private Chamado(
      String numero,
      String titulo,
      String descricao,
      Long solicitanteId,
      Long abertoPorId,
      Long categoriaId,
      Prioridade prioridadeSugerida,
      Instant agora) {
    this.numero = numero;
    this.titulo = titulo.trim();
    this.descricao = descricao.trim();
    this.solicitanteId = solicitanteId;
    this.abertoPorId = abertoPorId;
    this.categoriaId = categoriaId;
    this.prioridade = Prioridade.MEDIA;
    this.prioridadeSugerida = prioridadeSugerida;
    this.status = StatusChamado.ABERTO;
    this.canal = "PORTAL";
    this.criadoEm = agora;
    this.atualizadoEm = agora;
  }

  public static Chamado abrir(
      String numero,
      String titulo,
      String descricao,
      Long solicitanteId,
      Long abertoPorId,
      Long categoriaId,
      Prioridade prioridadeSugerida,
      Instant agora) {
    return new Chamado(
        numero,
        titulo,
        descricao,
        solicitanteId,
        abertoPorId,
        categoriaId,
        prioridadeSugerida,
        agora);
  }

  public Long getId() {
    return id;
  }

  public String getNumero() {
    return numero;
  }

  public String getTitulo() {
    return titulo;
  }

  public String getDescricao() {
    return descricao;
  }

  public Long getSolicitanteId() {
    return solicitanteId;
  }

  public Long getAbertoPorId() {
    return abertoPorId;
  }

  public Long getCategoriaId() {
    return categoriaId;
  }

  public Long getResponsavelId() {
    return responsavelId;
  }

  public Prioridade getPrioridade() {
    return prioridade;
  }

  public Prioridade getPrioridadeSugerida() {
    return prioridadeSugerida;
  }

  public StatusChamado getStatus() {
    return status;
  }

  public String getCanal() {
    return canal;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }

  public Instant getResolvidoEm() {
    return resolvidoEm;
  }

  public String getSolucao() {
    return solucao;
  }

  public Instant getPrazoResolucao() {
    return prazoResolucao;
  }

  public Long getVersion() {
    return version;
  }

  public void assumir(Long agenteId, Instant agora) {
    if (responsavelId != null
        || status == StatusChamado.RESOLVIDO
        || status == StatusChamado.FECHADO) {
      throw new ConflitoChamadoException("Este chamado já foi assumido ou está concluído");
    }
    responsavelId = agenteId;
    if (status == StatusChamado.ABERTO) status = StatusChamado.EM_ATENDIMENTO;
    atualizadoEm = agora;
  }

  public void atribuir(Long novoResponsavelId, Instant agora) {
    if (status == StatusChamado.RESOLVIDO || status == StatusChamado.FECHADO) {
      throw new ConflitoChamadoException("Chamado concluído não pode ser atribuído");
    }
    if (Objects.equals(responsavelId, novoResponsavelId)) return;
    responsavelId = novoResponsavelId;
    if (status == StatusChamado.ABERTO && novoResponsavelId != null) {
      status = StatusChamado.EM_ATENDIMENTO;
    }
    atualizadoEm = agora;
  }

  public void alterarStatus(StatusChamado novoStatus, String textoSolucao, Instant agora) {
    boolean permitido =
        (status == StatusChamado.ABERTO
                && novoStatus == StatusChamado.EM_ATENDIMENTO
                && responsavelId != null)
            || (status == StatusChamado.EM_ATENDIMENTO
                && (novoStatus == StatusChamado.AGUARDANDO_USUARIO
                    || novoStatus == StatusChamado.RESOLVIDO))
            || (status == StatusChamado.AGUARDANDO_USUARIO
                && novoStatus == StatusChamado.EM_ATENDIMENTO);
    if (!permitido) {
      throw new ConflitoChamadoException("Transição de status inválida");
    }
    if (novoStatus == StatusChamado.RESOLVIDO) {
      if (textoSolucao == null || textoSolucao.isBlank()) {
        throw new RequisicaoInvalidaException("Informe a solução antes de resolver o chamado");
      }
      solucao = textoSolucao.trim();
      resolvidoEm = agora;
    }
    status = novoStatus;
    atualizadoEm = agora;
  }

  public void alterarPrioridade(Prioridade novaPrioridade, Instant agora) {
    if (prioridade != novaPrioridade) {
      prioridade = novaPrioridade;
      atualizadoEm = agora;
    }
  }

  public void alterarCategoria(Long novaCategoriaId, Instant agora) {
    if (!categoriaId.equals(novaCategoriaId)) {
      categoriaId = novaCategoriaId;
      atualizadoEm = agora;
    }
  }
}
