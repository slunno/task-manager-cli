package br.com.empresa.helpdesk.conhecimento.application;

import br.com.empresa.helpdesk.admin.application.CategoriaService;
import br.com.empresa.helpdesk.compartilhado.erros.ConflitoChamadoException;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.conhecimento.domain.Artigo;
import br.com.empresa.helpdesk.conhecimento.domain.FiltroSalvo;
import br.com.empresa.helpdesk.conhecimento.domain.RespostaPronta;
import br.com.empresa.helpdesk.conhecimento.infra.ArtigoRepository;
import br.com.empresa.helpdesk.conhecimento.infra.FiltroSalvoRepository;
import br.com.empresa.helpdesk.conhecimento.infra.RespostaProntaRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConhecimentoService {
  private static final Set<String> FILTROS =
      Set.of(
          "status",
          "prioridade",
          "responsavelId",
          "categoriaId",
          "setorId",
          "desde",
          "ate",
          "texto",
          "semResponsavel",
          "slaVencendo",
          "meus",
          "sort");
  private final ArtigoRepository artigos;
  private final RespostaProntaRepository respostas;
  private final FiltroSalvoRepository filtros;
  private final CategoriaService categorias;
  private final Clock clock;

  public ConhecimentoService(
      ArtigoRepository artigos,
      RespostaProntaRepository respostas,
      FiltroSalvoRepository filtros,
      CategoriaService categorias,
      Clock clock) {
    this.artigos = artigos;
    this.respostas = respostas;
    this.filtros = filtros;
    this.categorias = categorias;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PaginaResponse<Artigo> buscarArtigos(
      String texto, Long categoriaId, boolean incluirRascunhos, int page, int size) {
    if (page < 0 || size < 1 || size > 50 || (texto != null && texto.length() > 120))
      throw new RequisicaoInvalidaException("Busca ou paginação inválida");
    return PaginaResponse.de(
        artigos.buscar(
            texto == null ? "" : texto.trim().toLowerCase(),
            categoriaId,
            incluirRascunhos,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "atualizadoEm", "id"))));
  }

  @Transactional(readOnly = true)
  public Artigo artigo(Long id, boolean podeVerRascunho) {
    Artigo artigo =
        artigos
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Artigo não encontrado"));
    if (!artigo.isPublicado() && !podeVerRascunho)
      throw new RecursoNaoEncontradoException("Artigo não encontrado");
    return artigo;
  }

  @Transactional
  public Artigo criarArtigo(
      String titulo, String conteudo, Long categoriaId, boolean publicado, Long autorId) {
    if (categoriaId != null) categorias.exigirAtiva(categoriaId);
    return artigos.saveAndFlush(
        new Artigo(
            titulo.trim(), conteudo.trim(), categoriaId, publicado, autorId, Instant.now(clock)));
  }

  @Transactional
  public Artigo editarArtigo(
      Long id, String titulo, String conteudo, Long categoriaId, boolean publicado) {
    Artigo artigo = artigo(id, true);
    if (categoriaId != null) categorias.exigirAtiva(categoriaId);
    artigo.editar(titulo.trim(), conteudo.trim(), categoriaId, publicado, Instant.now(clock));
    return artigos.saveAndFlush(artigo);
  }

  @Transactional(readOnly = true)
  public List<RespostaPronta> respostas() {
    return respostas.findByAtivoTrueOrderByTituloAsc();
  }

  @Transactional
  public RespostaPronta criarResposta(String titulo, String texto, Long autorId) {
    return respostas.saveAndFlush(
        new RespostaPronta(titulo.trim(), texto.trim(), autorId, Instant.now(clock)));
  }

  @Transactional
  public RespostaPronta editarResposta(Long id, String titulo, String texto, boolean ativo) {
    RespostaPronta resposta =
        respostas
            .findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Resposta não encontrada"));
    resposta.editar(titulo.trim(), texto.trim(), ativo, Instant.now(clock));
    return respostas.saveAndFlush(resposta);
  }

  @Transactional(readOnly = true)
  public List<FiltroSalvo> filtros(Long usuarioId) {
    return filtros.findByUsuarioIdOrderByNomeAsc(usuarioId);
  }

  @Transactional
  public FiltroSalvo salvarFiltro(Long usuarioId, String nome, String parametros) {
    String limpo = parametros.startsWith("?") ? parametros.substring(1) : parametros;
    if (limpo.isBlank()
        || limpo.length() > 1000
        || Arrays.stream(limpo.split("&"))
            .map(par -> par.split("=", 2)[0])
            .anyMatch(chave -> !FILTROS.contains(chave)))
      throw new RequisicaoInvalidaException("Parâmetros do filtro inválidos");
    if (filtros.countByUsuarioId(usuarioId) >= 20)
      throw new RequisicaoInvalidaException("Limite de 20 filtros salvos atingido");
    if (filtros.existsByUsuarioIdAndNome(usuarioId, nome.trim()))
      throw new ConflitoChamadoException("Já existe um filtro com este nome");
    return filtros.saveAndFlush(new FiltroSalvo(usuarioId, nome.trim(), limpo, Instant.now(clock)));
  }

  @Transactional
  public void excluirFiltro(Long id, Long usuarioId) {
    FiltroSalvo filtro =
        filtros
            .findByIdAndUsuarioId(id, usuarioId)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Filtro não encontrado"));
    filtros.delete(filtro);
  }
}
