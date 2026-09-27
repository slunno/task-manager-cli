package br.com.empresa.helpdesk.usuarios.api;

import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.usuarios.application.UsuarioAdminService;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ti/admin/usuarios")
@PreAuthorize("hasRole('TI_ADMIN')")
public class UsuarioAdminController {
  private final UsuarioAdminService service;

  public UsuarioAdminController(UsuarioAdminService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<PaginaResponse<UsuarioAdminResponse>> listar(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(PaginaResponse.de(service.listar(page, size).map(UsuarioAdminResponse::de)));
  }

  @PatchMapping("/{id}")
  public ResponseEntity<UsuarioAdminResponse> atualizar(
      @PathVariable Long id, @Valid @RequestBody AtualizarUsuarioRequest dados) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            UsuarioAdminResponse.de(
                service.atualizar(id, dados.nome(), dados.email(), dados.perfil(), dados.ativo())));
  }

  @PostMapping(path = "/importacao", consumes = "multipart/form-data")
  public ResponseEntity<UsuarioAdminService.RelatorioImportacao> importar(
      @RequestPart("arquivo") MultipartFile arquivo) throws IOException {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.importar(arquivo.getBytes()));
  }

  public record AtualizarUsuarioRequest(
      @NotBlank @Size(max = 180) String nome,
      @NotBlank @Email @Size(max = 254) String email,
      @NotNull Perfil perfil,
      boolean ativo) {}

  public record UsuarioAdminResponse(
      Long id, String nome, String email, Perfil perfil, boolean ativo) {
    static UsuarioAdminResponse de(Usuario usuario) {
      return new UsuarioAdminResponse(
          usuario.getId(),
          usuario.getNome(),
          usuario.getEmail(),
          usuario.getPerfil(),
          usuario.isAtivo());
    }
  }
}
