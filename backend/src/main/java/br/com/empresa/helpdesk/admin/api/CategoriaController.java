package br.com.empresa.helpdesk.admin.api;

import br.com.empresa.helpdesk.admin.application.CategoriaService;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CategoriaController {
  private final CategoriaService categorias;

  public CategoriaController(CategoriaService categorias) {
    this.categorias = categorias;
  }

  @GetMapping("/api/v1/categorias")
  @PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<List<CategoriaResponse>> listar() {
    var dados =
        categorias.listarAtivas().stream()
            .map(categoria -> new CategoriaResponse(categoria.getId(), categoria.getNome()))
            .toList();
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dados);
  }

  public record CategoriaResponse(
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Long id,
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String nome) {}
}
