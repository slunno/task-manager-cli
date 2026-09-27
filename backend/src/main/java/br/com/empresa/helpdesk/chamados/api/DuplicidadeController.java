package br.com.empresa.helpdesk.chamados.api;

import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ti/chamados/{id}/duplicidade")
@PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
public class DuplicidadeController {
  private final ChamadoService service;

  public DuplicidadeController(ChamadoService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<ChamadoService.Duplicidade> obter(@PathVariable Long id) {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.duplicidade(id));
  }

  @PutMapping
  public ResponseEntity<ChamadoService.Duplicidade> vincular(
      @PathVariable Long id,
      @Valid @RequestBody VincularRequest dados,
      HttpServletRequest request) {
    Usuario ator = (Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.vincularDuplicado(id, dados.principalId(), dados.version(), ator));
  }

  public record VincularRequest(Long principalId, @NotNull Long version) {}
}
