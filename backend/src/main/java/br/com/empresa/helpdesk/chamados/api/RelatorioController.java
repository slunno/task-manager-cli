package br.com.empresa.helpdesk.chamados.api;

import br.com.empresa.helpdesk.chamados.application.RelatorioService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ti/relatorios")
@PreAuthorize("hasAnyRole('TI_AGENTE', 'TI_ADMIN')")
public class RelatorioController {
  private final RelatorioService service;

  public RelatorioController(RelatorioService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<RelatorioService.Relatorio> consultar(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
      @RequestParam(required = false) Long setorId,
      @RequestParam(required = false) Long categoriaId) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.consultar(desde, ate, setorId, categoriaId));
  }

  @GetMapping(value = "/exportacao.csv", produces = "text/csv")
  public ResponseEntity<byte[]> exportar(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
      @RequestParam(required = false) Long setorId,
      @RequestParam(required = false) Long categoriaId) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=relatorio-chamados.csv")
        .body(service.csv(service.consultar(desde, ate, setorId, categoriaId)));
  }
}
