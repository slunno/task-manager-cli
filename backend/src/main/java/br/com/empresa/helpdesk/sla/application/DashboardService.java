package br.com.empresa.helpdesk.sla.application;

import br.com.empresa.helpdesk.sla.api.DashboardResponse;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
  private final JdbcTemplate jdbc;
  private final Clock clock;

  public DashboardService(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public DashboardResponse consultar() {
    Map<String, Long> porStatus = contagens("status");
    Map<String, Long> porPrioridade = contagens("prioridade");
    Instant agora = Instant.now(clock);
    Long total = jdbc.queryForObject("select count(*) from chamados", Long.class);
    Long aberto =
        jdbc.queryForObject(
            "select count(*) from chamados where status not in ('RESOLVIDO','FECHADO')",
            Long.class);
    Long vencidos =
        jdbc.queryForObject(
            "select count(*) from chamados where status not in ('RESOLVIDO','FECHADO') and sla_pausado_em is null and prazo_resolucao < ?",
            Long.class,
            java.sql.Timestamp.from(agora));
    Long vencendo =
        jdbc.queryForObject(
            "select count(*) from chamados where status not in ('RESOLVIDO','FECHADO') and sla_pausado_em is null and prazo_resolucao between ? and ?",
            Long.class,
            java.sql.Timestamp.from(agora),
            java.sql.Timestamp.from(agora.plusSeconds(3600)));
    Double media =
        jdbc.queryForObject(
            "select avg(extract(epoch from (resolvido_em - criado_em))) / 3600 from chamados where resolvido_em is not null",
            Double.class);
    return new DashboardResponse(
        total, aberto, vencidos, vencendo, media, porStatus, porPrioridade);
  }

  private Map<String, Long> contagens(String coluna) {
    Map<String, Long> resultado = new LinkedHashMap<>();
    jdbc.query(
        "select " + coluna + ", count(*) from chamados group by " + coluna,
        (ResultSet rs) -> {
          resultado.put(rs.getString(1), rs.getLong(2));
        });
    return resultado;
  }
}
