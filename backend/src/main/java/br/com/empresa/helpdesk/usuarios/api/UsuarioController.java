package br.com.empresa.helpdesk.usuarios.api;

import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuarioController {
  private final UsuarioMapper mapper;

  public UsuarioController(UsuarioMapper mapper) {
    this.mapper = mapper;
  }

  @GetMapping("/api/v1/me")
  public ResponseEntity<UsuarioResponse> me(HttpServletRequest request) {
    Usuario usuario = (Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(mapper.paraResponse(usuario));
  }
}
