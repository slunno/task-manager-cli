package br.com.empresa.helpdesk.admin.api;

import br.com.empresa.helpdesk.admin.application.CategoriaAdminService;
import br.com.empresa.helpdesk.admin.domain.Categoria;
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
@RequestMapping("/api/v1/ti/admin/categorias")
@PreAuthorize("hasRole('TI_ADMIN')")
public class CategoriaAdminController {
  private final CategoriaAdminService service;

  public CategoriaAdminController(CategoriaAdminService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<CategoriaAdminResponse>> listar() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.listar().stream().map(CategoriaAdminResponse::de).toList());
  }

  @PostMapping
  public ResponseEntity<CategoriaAdminResponse> criar(
      @Valid @RequestBody CriarCategoriaRequest dados) {
    var criada = service.criar(dados.nome());
    return ResponseEntity.created(URI.create("/api/v1/ti/admin/categorias/" + criada.getId()))
        .cacheControl(CacheControl.noStore())
        .body(CategoriaAdminResponse.de(criada));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<CategoriaAdminResponse> atualizar(
      @PathVariable Long id, @Valid @RequestBody AtualizarCategoriaRequest dados) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(CategoriaAdminResponse.de(service.atualizar(id, dados.nome(), dados.ativa())));
  }

  public record CriarCategoriaRequest(@NotBlank @Size(max = 120) String nome) {}

  public record AtualizarCategoriaRequest(@NotBlank @Size(max = 120) String nome, boolean ativa) {}

  public record CategoriaAdminResponse(Long id, String nome, boolean ativa) {
    static CategoriaAdminResponse de(Categoria categoria) {
      return new CategoriaAdminResponse(
          categoria.getId(), categoria.getNome(), categoria.isAtiva());
    }
  }
}
