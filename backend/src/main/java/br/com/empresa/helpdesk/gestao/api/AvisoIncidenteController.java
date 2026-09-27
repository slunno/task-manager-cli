package br.com.empresa.helpdesk.gestao.api;

import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.gestao.application.AvisoIncidenteService;
import br.com.empresa.helpdesk.gestao.domain.AvisoIncidente;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AvisoIncidenteController {
  private final AvisoIncidenteService service;

  public AvisoIncidenteController(AvisoIncidenteService service) {
    this.service = service;
  }

  @GetMapping("/avisos")
  @PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
  public ResponseEntity<List<AvisoResponse>> vigentes() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.vigentes().stream().map(AvisoResponse::de).toList());
  }

  @GetMapping("/ti/admin/avisos")
  @PreAuthorize("hasRole('TI_ADMIN')")
  public ResponseEntity<List<AvisoResponse>> todos() {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.todos().stream().map(AvisoResponse::de).toList());
  }

  @PostMapping("/ti/admin/avisos")
  @PreAuthorize("hasRole('TI_ADMIN')")
  public ResponseEntity<AvisoResponse> criar(
      @Valid @RequestBody AvisoRequest dados, HttpServletRequest request) {
    Long autorId = ((Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO)).getId();
    AvisoIncidente aviso =
        service.criar(dados.titulo(), dados.mensagem(), dados.inicioEm(), dados.fimEm(), autorId);
    return ResponseEntity.created(URI.create("/api/v1/ti/admin/avisos/" + aviso.getId()))
        .body(AvisoResponse.de(aviso));
  }

  @PutMapping("/ti/admin/avisos/{id}")
  @PreAuthorize("hasRole('TI_ADMIN')")
  public ResponseEntity<AvisoResponse> editar(
      @PathVariable Long id, @Valid @RequestBody AvisoRequest dados) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            AvisoResponse.de(
                service.editar(
                    id,
                    dados.titulo(),
                    dados.mensagem(),
                    dados.inicioEm(),
                    dados.fimEm(),
                    dados.ativo())));
  }

  public record AvisoRequest(
      @NotBlank @Size(max = 160) String titulo,
      @NotBlank @Size(max = 1000) String mensagem,
      @NotNull Instant inicioEm,
      Instant fimEm,
      boolean ativo) {}

  public record AvisoResponse(
      Long id, String titulo, String mensagem, boolean ativo, Instant inicioEm, Instant fimEm) {
    static AvisoResponse de(AvisoIncidente aviso) {
      return new AvisoResponse(
          aviso.getId(),
          aviso.getTitulo(),
          aviso.getMensagem(),
          aviso.isAtivo(),
          aviso.getInicioEm(),
          aviso.getFimEm());
    }
  }
}
