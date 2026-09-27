package br.com.empresa.helpdesk.sla;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.helpdesk.sla.application.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DashboardServiceTest {
  @Autowired private DashboardService dashboard;

  @Test
  void consultaAgregadosNoBanco() {
    var indicadores = dashboard.consultar();
    assertThat(indicadores.totalChamados()).isGreaterThanOrEqualTo(0);
    assertThat(indicadores.porStatus()).isNotNull();
  }
}
