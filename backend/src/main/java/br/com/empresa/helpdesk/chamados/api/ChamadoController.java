package br.com.empresa.helpdesk.chamados.api;

import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chamados")
public class ChamadoController {
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

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<ChamadoResponse> detalhe(
      @PathVariable Long id, HttpServletRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(chamados.detalhe(id, usuario(request)));
  }

  private Usuario usuario(HttpServletRequest request) {
    return (Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO);
  }
}
