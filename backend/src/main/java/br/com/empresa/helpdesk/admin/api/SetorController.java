package br.com.empresa.helpdesk.admin.api;

import br.com.empresa.helpdesk.admin.application.SetorService;
import br.com.empresa.helpdesk.admin.domain.Setor;
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
@RequestMapping("/api/v1/ti")
public class SetorController {
  private final SetorService service;

  public SetorController(SetorService service) {
    this.service = service;
  }

  @GetMapping("/setores")
  @PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<List<SetorResponse>> ativos() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.ativos().stream().map(SetorResponse::de).toList());
  }

  @GetMapping("/admin/setores")
  @PreAuthorize("hasRole('TI_ADMIN')")
  public ResponseEntity<List<SetorResponse>> todos() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.todos().stream().map(SetorResponse::de).toList());
  }

  @PostMapping("/admin/setores")
  @PreAuthorize("hasRole('TI_ADMIN')")
  public ResponseEntity<SetorResponse> criar(@Valid @RequestBody NovoSetorRequest dados) {
    Setor setor = service.criar(dados.nome());
    return ResponseEntity.created(URI.create("/api/v1/ti/admin/setores/" + setor.getId()))
        .body(SetorResponse.de(setor));
  }

  @PutMapping("/admin/setores/{id}")
  @PreAuthorize("hasRole('TI_ADMIN')")
  public ResponseEntity<SetorResponse> editar(
      @PathVariable Long id, @Valid @RequestBody EditarSetorRequest dados) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(SetorResponse.de(service.editar(id, dados.nome(), dados.ativo())));
  }

  public record NovoSetorRequest(@NotBlank @Size(max = 120) String nome) {}

  public record EditarSetorRequest(@NotBlank @Size(max = 120) String nome, boolean ativo) {}

  public record SetorResponse(Long id, String nome, boolean ativo) {
    static SetorResponse de(Setor setor) {
      return new SetorResponse(setor.getId(), setor.getNome(), setor.isAtivo());
    }
  }
}
