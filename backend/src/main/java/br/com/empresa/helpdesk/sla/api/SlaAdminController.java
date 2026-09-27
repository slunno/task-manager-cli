package br.com.empresa.helpdesk.sla.api;

import br.com.empresa.helpdesk.chamados.domain.Prioridade;
import br.com.empresa.helpdesk.sla.application.SlaAdminService;
import br.com.empresa.helpdesk.sla.application.SlaAdminService.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ti/admin")
@PreAuthorize("hasRole('TI_ADMIN')")
public class SlaAdminController {
  private final SlaAdminService service;

  public SlaAdminController(SlaAdminService service) {
    this.service = service;
  }

  @GetMapping("/slas")
  public ResponseEntity<List<Politica>> politicas() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.politicas());
  }

  @PutMapping("/slas/{prioridade}")
  public ResponseEntity<Politica> atualizarPolitica(
      @PathVariable Prioridade prioridade, @Valid @RequestBody PoliticaRequest dados) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(
            service.atualizarPolitica(
                prioridade, dados.horasPrimeiraResposta(), dados.horasResolucao()));
  }

  @GetMapping("/calendario")
  public ResponseEntity<Calendario> calendario() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.calendario());
  }

  @PutMapping("/calendario/expediente")
  public ResponseEntity<Calendario> expediente(@RequestBody List<Janela> janelas) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.atualizarExpediente(janelas));
  }

  @PostMapping("/calendario/feriados")
  public ResponseEntity<Feriado> adicionarFeriado(@Valid @RequestBody FeriadoRequest dados) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(service.adicionarFeriado(dados.data(), dados.descricao()));
  }

  @DeleteMapping("/calendario/feriados/{id}")
  public ResponseEntity<Void> removerFeriado(@PathVariable long id) {
    service.removerFeriado(id);
    return ResponseEntity.noContent().build();
  }

  public record PoliticaRequest(@Min(1) int horasPrimeiraResposta, @Min(1) int horasResolucao) {}

  public record FeriadoRequest(
      @NotNull LocalDate data, @NotBlank @Size(max = 180) String descricao) {}
}
