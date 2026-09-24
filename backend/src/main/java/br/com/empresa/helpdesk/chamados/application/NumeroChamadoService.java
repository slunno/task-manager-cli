package br.com.empresa.helpdesk.chamados.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NumeroChamadoService {
  private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public NumeroChamadoService(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  public String proximo() {
    Long sequencia = jdbc.queryForObject("SELECT nextval('chamado_numero_seq')", Long.class);
    int ano = LocalDate.now(clock.withZone(FUSO)).getYear();
    return String.format(Locale.ROOT, "CH-%d-%06d", ano, sequencia);
  }
}
