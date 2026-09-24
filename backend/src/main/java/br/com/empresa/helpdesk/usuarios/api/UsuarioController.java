package br.com.empresa.helpdesk.usuarios.api;

import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsuarioController {
  private final UsuarioMapper mapper;
  private final UsuarioService usuarios;

  public UsuarioController(UsuarioMapper mapper, UsuarioService usuarios) {
    this.mapper = mapper;
    this.usuarios = usuarios;
  }

  @GetMapping("/api/v1/me")
  public ResponseEntity<UsuarioResponse> me(HttpServletRequest request) {
    Usuario usuario = (Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(mapper.paraResponse(usuario));
  }

  @GetMapping("/api/v1/usuarios/busca")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<List<UsuarioBuscaResponse>> buscar(
      @RequestParam String texto, @RequestParam(defaultValue = "false") boolean somenteTi) {
    List<UsuarioBuscaResponse> encontrados =
        usuarios.buscarAtivos(texto, somenteTi).stream()
            .map(
                usuario ->
                    new UsuarioBuscaResponse(
                        usuario.getId(),
                        usuario.getNome(),
                        usuario.getEmail(),
                        usuario.getPerfil()))
            .toList();
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(encontrados);
  }
}
