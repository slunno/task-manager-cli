package br.com.empresa.helpdesk.anexos.api;

import br.com.empresa.helpdesk.anexos.application.AnexoService;
import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
public class AnexoController {
  private final AnexoService anexos;

  public AnexoController(AnexoService anexos) {
    this.anexos = anexos;
  }

  @PostMapping(
      value = "/api/v1/chamados/{chamadoId}/anexos",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<AnexoResponse> enviar(
      @PathVariable Long chamadoId,
      @RequestParam(required = false) Long comentarioId,
      @RequestParam(defaultValue = "false") boolean interno,
      @RequestPart("arquivo") MultipartFile arquivo,
      HttpServletRequest request) {
    var criado = anexos.enviar(chamadoId, comentarioId, interno, arquivo, usuario(request));
    return ResponseEntity.created(URI.create("/api/v1/anexos/" + criado.id() + "/download"))
        .cacheControl(CacheControl.noStore())
        .body(criado);
  }

  @GetMapping("/api/v1/chamados/{chamadoId}/anexos")
  public ResponseEntity<PaginaResponse<AnexoResponse>> listar(
      @PathVariable Long chamadoId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      HttpServletRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(anexos.listar(chamadoId, usuario(request), page, size));
  }

  @GetMapping("/api/v1/anexos/{id}/download")
  public ResponseEntity<byte[]> baixar(@PathVariable Long id, HttpServletRequest request) {
    var arquivo = anexos.baixar(id, usuario(request));
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(arquivo.nome(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("X-Content-Type-Options", "nosniff")
        .contentType(MediaType.parseMediaType(arquivo.mime()))
        .contentLength(arquivo.dados().length)
        .body(arquivo.dados());
  }

  private Usuario usuario(HttpServletRequest request) {
    return (Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO);
  }
}
