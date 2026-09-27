package br.com.empresa.helpdesk.conhecimento.api;

import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.conhecimento.application.ConhecimentoService;
import br.com.empresa.helpdesk.conhecimento.domain.Artigo;
import br.com.empresa.helpdesk.conhecimento.domain.FiltroSalvo;
import br.com.empresa.helpdesk.conhecimento.domain.RespostaPronta;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ConhecimentoController {
  private final ConhecimentoService service;

  public ConhecimentoController(ConhecimentoService service) {
    this.service = service;
  }

  @GetMapping("/artigos")
  @PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<PaginaResponse<ArtigoResponse>> artigos(
      @RequestParam(defaultValue = "") String texto,
      @RequestParam(required = false) Long categoriaId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      HttpServletRequest request) {
    boolean ti = ti(usuario(request));
    var pagina = service.buscarArtigos(texto, categoriaId, ti, page, size);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            new PaginaResponse<>(
                pagina.content().stream().map(ArtigoResponse::de).toList(),
                pagina.page(),
                pagina.size(),
                pagina.totalElements(),
                pagina.totalPages()));
  }

  @GetMapping("/artigos/{id}")
  @PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<ArtigoResponse> artigo(@PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(ArtigoResponse.de(service.artigo(id, ti(usuario(request)))));
  }

  @PostMapping("/ti/artigos")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<ArtigoResponse> criarArtigo(
      @Valid @RequestBody ArtigoRequest dados, HttpServletRequest request) {
    Artigo artigo =
        service.criarArtigo(
            dados.titulo(),
            dados.conteudo(),
            dados.categoriaId(),
            dados.publicado(),
            usuario(request).getId());
    return ResponseEntity.created(URI.create("/api/v1/artigos/" + artigo.getId()))
        .body(ArtigoResponse.de(artigo));
  }

  @PutMapping("/ti/artigos/{id}")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<ArtigoResponse> editarArtigo(
      @PathVariable Long id, @Valid @RequestBody ArtigoRequest dados) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            ArtigoResponse.de(
                service.editarArtigo(
                    id, dados.titulo(), dados.conteudo(), dados.categoriaId(), dados.publicado())));
  }

  @GetMapping("/ti/respostas-prontas")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<List<RespostaResponse>> respostas() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.respostas().stream().map(RespostaResponse::de).toList());
  }

  @PostMapping("/ti/respostas-prontas")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<RespostaResponse> criarResposta(
      @Valid @RequestBody RespostaRequest dados, HttpServletRequest request) {
    RespostaPronta resposta =
        service.criarResposta(dados.titulo(), dados.texto(), usuario(request).getId());
    return ResponseEntity.created(URI.create("/api/v1/ti/respostas-prontas/" + resposta.getId()))
        .body(RespostaResponse.de(resposta));
  }

  @PutMapping("/ti/respostas-prontas/{id}")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<RespostaResponse> editarResposta(
      @PathVariable Long id, @Valid @RequestBody RespostaRequest dados) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            RespostaResponse.de(
                service.editarResposta(id, dados.titulo(), dados.texto(), dados.ativo())));
  }

  @GetMapping("/ti/filtros-salvos")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<List<FiltroResponse>> filtros(HttpServletRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.filtros(usuario(request).getId()).stream().map(FiltroResponse::de).toList());
  }

  @PostMapping("/ti/filtros-salvos")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<FiltroResponse> salvarFiltro(
      @Valid @RequestBody FiltroRequest dados, HttpServletRequest request) {
    FiltroSalvo filtro =
        service.salvarFiltro(usuario(request).getId(), dados.nome(), dados.parametros());
    return ResponseEntity.created(URI.create("/api/v1/ti/filtros-salvos/" + filtro.getId()))
        .body(FiltroResponse.de(filtro));
  }

  @DeleteMapping("/ti/filtros-salvos/{id}")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<Void> excluirFiltro(@PathVariable Long id, HttpServletRequest request) {
    service.excluirFiltro(id, usuario(request).getId());
    return ResponseEntity.noContent().build();
  }

  private Usuario usuario(HttpServletRequest request) {
    return (Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO);
  }

  private boolean ti(Usuario usuario) {
    return usuario.getPerfil() != Perfil.FUNCIONARIO;
  }

  public record ArtigoRequest(
      @NotBlank @Size(min = 5, max = 200) String titulo,
      @NotBlank @Size(min = 20, max = 20000) String conteudo,
      Long categoriaId,
      boolean publicado) {}

  public record ArtigoResponse(
      Long id,
      String titulo,
      String conteudo,
      Long categoriaId,
      boolean publicado,
      java.time.Instant atualizadoEm) {
    static ArtigoResponse de(Artigo artigo) {
      return new ArtigoResponse(
          artigo.getId(),
          artigo.getTitulo(),
          artigo.getConteudo(),
          artigo.getCategoriaId(),
          artigo.isPublicado(),
          artigo.getAtualizadoEm());
    }
  }

  public record RespostaRequest(
      @NotBlank @Size(max = 120) String titulo,
      @NotBlank @Size(max = 10000) String texto,
      boolean ativo) {}

  public record RespostaResponse(Long id, String titulo, String texto, boolean ativo) {
    static RespostaResponse de(RespostaPronta resposta) {
      return new RespostaResponse(
          resposta.getId(), resposta.getTitulo(), resposta.getTexto(), resposta.isAtivo());
    }
  }

  public record FiltroRequest(
      @NotBlank @Size(max = 80) String nome, @NotBlank @Size(max = 1000) String parametros) {}

  public record FiltroResponse(Long id, String nome, String parametros) {
    static FiltroResponse de(FiltroSalvo filtro) {
      return new FiltroResponse(filtro.getId(), filtro.getNome(), filtro.getParametros());
    }
  }
}
