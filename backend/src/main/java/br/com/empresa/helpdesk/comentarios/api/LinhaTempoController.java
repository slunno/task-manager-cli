package br.com.empresa.helpdesk.comentarios.api;

import br.com.empresa.helpdesk.comentarios.application.LinhaTempoService;
import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chamados/{chamadoId}/linha-do-tempo")
@PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
public class LinhaTempoController {
  private final LinhaTempoService linhaTempo;

  public LinhaTempoController(LinhaTempoService linhaTempo) {
    this.linhaTempo = linhaTempo;
  }

  @GetMapping
  public ResponseEntity<PaginaResponse<LinhaTempoItemResponse>> listar(
      @PathVariable Long chamadoId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest request) {
    Usuario ator = (Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(linhaTempo.listar(chamadoId, ator, page, size));
  }
}
