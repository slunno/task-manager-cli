package br.com.empresa.helpdesk.sla.api;

import br.com.empresa.helpdesk.sla.application.DashboardService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ti/dashboard")
public class DashboardController {
  private final DashboardService service;

  public DashboardController(DashboardService service) {
    this.service = service;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('TI_AGENTE','TI_ADMIN')")
  public ResponseEntity<DashboardResponse> consultar() {
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.consultar());
  }
}
