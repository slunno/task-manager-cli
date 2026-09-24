package br.com.empresa.helpdesk.chamados.api;

import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.chamados.application.FiltroFila;
import br.com.empresa.helpdesk.chamados.domain.Prioridade;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.historico.api.HistoricoResponse;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chamados")
public class ChamadoController {
  private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
  private final ChamadoService chamados;

  public ChamadoController(ChamadoService chamados) {
    this.chamados = chamados;
  }

  @PostMapping
  @ApiResponse(responseCode = "201", description = "Chamado criado")
  @PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<ChamadoResponse> criar(
      @Valid @RequestBody CriarChamadoRequest dados, HttpServletRequest request) {
    ChamadoResponse criado = chamados.criar(dados, usuario(request));
    return ResponseEntity.created(URI.create("/api/v1/chamados/" + criado.id()))
        .cacheControl(CacheControl.noStore())
        .body(criado);
  }

  @GetMapping("/meus")
  @PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<PaginaResponse<ChamadoResponse>> meus(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "criadoEm,desc") String sort,
      HttpServletRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(chamados.meus(usuario(request), page, size, sort));
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<PaginaResponse<ChamadoResponse>> fila(
      @RequestParam(required = false) StatusChamado status,
      @RequestParam(required = false) Prioridade prioridade,
      @RequestParam(required = false) Long responsavelId,
      @RequestParam(required = false) Long categoriaId,
      @RequestParam(required = false) Long setorId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
      @RequestParam(required = false) String texto,
      @RequestParam(defaultValue = "false") boolean semResponsavel,
      @RequestParam(defaultValue = "false") boolean meus,
      @RequestParam(defaultValue = "false") boolean slaVencendo,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "criadoEm,desc") String sort,
      HttpServletRequest request) {
    var filtro =
        new FiltroFila(
            status,
            prioridade,
            responsavelId,
            categoriaId,
            setorId,
            desde == null ? null : desde.atStartOfDay(ZONA).toInstant(),
            ate == null ? null : ate.plusDays(1).atStartOfDay(ZONA).toInstant(),
            texto,
            semResponsavel,
            meus,
            slaVencendo);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(chamados.fila(usuario(request), filtro, page, size, sort));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<ChamadoResponse> detalhe(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(chamados.detalhe(id, usuario(request)));
  }

  @PostMapping("/{id}/assumir")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<ChamadoResponse> assumir(
      @PathVariable Long id,
      @Valid @RequestBody AssumirChamadoRequest dados,
      HttpServletRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(chamados.assumir(id, dados.version(), usuario(request)));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<ChamadoResponse> atualizar(
      @PathVariable Long id,
      @Valid @RequestBody AtualizarChamadoRequest dados,
      HttpServletRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(chamados.atualizar(id, dados, usuario(request)));
  }

  @GetMapping("/{id}/historico")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<PaginaResponse<HistoricoResponse>> historico(
      @PathVariable Long id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(chamados.historico(id, page, size));
  }

  private Usuario usuario(HttpServletRequest request) {
    return (Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO);
  }
}
