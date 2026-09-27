package br.com.empresa.helpdesk.comentarios.api;

import br.com.empresa.helpdesk.comentarios.application.ComentarioService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chamados/{chamadoId}/comentarios")
@PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
public class ComentarioController {
  private final ComentarioService comentarios;

  public ComentarioController(ComentarioService comentarios) {
    this.comentarios = comentarios;
  }

  @PostMapping
  @ApiResponse(responseCode = "201", description = "Comentário criado")
  public ResponseEntity<ComentarioResponse> criar(
      @PathVariable Long chamadoId,
      @Valid @RequestBody CriarComentarioRequest dados,
      HttpServletRequest request) {
    var criado = comentarios.criar(chamadoId, dados, usuario(request));
    return ResponseEntity.created(
            URI.create("/api/v1/chamados/" + chamadoId + "/comentarios/" + criado.id()))
        .cacheControl(CacheControl.noStore())
        .body(criado);
  }

  @GetMapping
  public ResponseEntity<PaginaResponse<ComentarioResponse>> listar(
      @PathVariable Long chamadoId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(comentarios.listar(chamadoId, usuario(request), page, size));
  }

  private Usuario usuario(HttpServletRequest request) {
    return (Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO);
  }
}
