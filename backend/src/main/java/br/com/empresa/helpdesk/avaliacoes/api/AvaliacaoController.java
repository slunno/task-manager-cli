package br.com.empresa.helpdesk.avaliacoes.api;

import br.com.empresa.helpdesk.avaliacoes.application.AvaliacaoService;
import br.com.empresa.helpdesk.avaliacoes.domain.Avaliacao;
import br.com.empresa.helpdesk.compartilhado.seguranca.UsuarioSessaoFilter;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chamados/{chamadoId}/avaliacao")
@PreAuthorize("hasAnyRole('FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN')")
public class AvaliacaoController {
  private final AvaliacaoService service;

  public AvaliacaoController(AvaliacaoService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<AvaliacaoResponse> buscar(
      @PathVariable Long chamadoId, HttpServletRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(AvaliacaoResponse.de(service.buscar(chamadoId, usuario(request))));
  }

  @PostMapping
  public ResponseEntity<AvaliacaoResponse> avaliar(
      @PathVariable Long chamadoId,
      @Valid @RequestBody AvaliarRequest dados,
      HttpServletRequest request) {
    Avaliacao criada =
        service.avaliar(chamadoId, dados.nota(), dados.comentario(), usuario(request));
    return ResponseEntity.created(URI.create("/api/v1/chamados/" + chamadoId + "/avaliacao"))
        .cacheControl(CacheControl.noStore())
        .body(AvaliacaoResponse.de(criada));
  }

  private Usuario usuario(HttpServletRequest request) {
    return (Usuario) request.getAttribute(UsuarioSessaoFilter.ATRIBUTO_USUARIO);
  }

  public record AvaliarRequest(@Min(1) @Max(5) int nota, @Size(max = 1000) String comentario) {}

  public record AvaliacaoResponse(Long chamadoId, int nota, String comentario, Instant criadoEm) {
    static AvaliacaoResponse de(Avaliacao avaliacao) {
      return new AvaliacaoResponse(
          avaliacao.getChamadoId(),
          avaliacao.getNota(),
          avaliacao.getComentario(),
          avaliacao.getCriadoEm());
    }
  }
}
