package br.com.empresa.helpdesk.sla;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.helpdesk.sla.domain.CalendarioUtil;
import br.com.empresa.helpdesk.sla.domain.CalendarioUtil.Janela;
import java.time.*;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CalendarioUtilTest {
  private final ZoneId zona = ZoneId.of("America/Sao_Paulo");
  private final CalendarioUtil calendario =
      new CalendarioUtil(
          zona,
          List.of(
              new Janela(DayOfWeek.FRIDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
              new Janela(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(18, 0)),
              new Janela(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(18, 0))),
          Set.of(LocalDate.of(2026, 9, 7)));

  @Test
  void pulaFimDeSemanaEFeriado() {
    Instant sexta = LocalDateTime.of(2026, 9, 4, 17, 0).atZone(zona).toInstant();
    Instant terca = LocalDateTime.of(2026, 9, 8, 10, 0).atZone(zona).toInstant();
    assertThat(calendario.adicionarMinutosUteis(sexta, 120)).isEqualTo(terca);
    assertThat(calendario.minutosUteisEntre(sexta, terca)).isEqualTo(120);
  }
}
