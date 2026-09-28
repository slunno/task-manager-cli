package br.com.empresa.helpdesk.sla.application;

import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.sla.api.DashboardResponse;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    return consultar(null, null);
  }

  @Transactional(readOnly = true)
  public DashboardResponse consultar(LocalDate desde, LocalDate ate) {
    ZoneId zona = ZoneId.of("America/Sao_Paulo");
    if (desde == null && ate == null) {
      ate = LocalDate.ofInstant(clock.instant(), zona);
      desde = ate.minusDays(29);
    }
    if (desde == null
        || ate == null
        || desde.isAfter(ate)
        || java.time.temporal.ChronoUnit.DAYS.between(desde, ate) > 365)
      throw new RequisicaoInvalidaException("Informe um período válido de até 366 dias");
    Timestamp inicio = Timestamp.from(desde.atStartOfDay(zona).toInstant());
    Timestamp fim = Timestamp.from(ate.plusDays(1).atStartOfDay(zona).toInstant());
    Instant agora = clock.instant();
    var indicadores =
        jdbc.queryForObject(
            "select count(*), coalesce(sum(case when status not in ('RESOLVIDO','FECHADO') then 1 else 0 end),0), coalesce(sum(case when status not in ('RESOLVIDO','FECHADO') and sla_pausado_em is null and prazo_resolucao < ? then 1 else 0 end),0), coalesce(sum(case when status not in ('RESOLVIDO','FECHADO') and sla_pausado_em is null and prazo_resolucao between ? and ? then 1 else 0 end),0), avg(case when resolvido_em is not null then resolucao_minutos_uteis end)/60.0, count(case when resolvido_em is not null then 1 end), count(case when resolvido_em is not null and resolvido_em <= prazo_resolucao then 1 end), count(case when resolvido_em is not null and resolucao_minutos_uteis is null then 1 end) from chamados where criado_em >= ? and criado_em < ?",
            (rs, i) -> {
              double media = rs.getDouble(5);
              Double mediaUtil = rs.wasNull() ? null : media;
              return new Agregado(
                  rs.getLong(1),
                  rs.getLong(2),
                  rs.getLong(3),
                  rs.getLong(4),
                  mediaUtil,
                  rs.getLong(6),
                  rs.getLong(7),
                  rs.getLong(8));
            },
            Timestamp.from(agora),
            Timestamp.from(agora),
            Timestamp.from(agora.plusSeconds(3600)),
            inicio,
            fim);
    Map<String, Long> categorias = new LinkedHashMap<>();
    jdbc.query(
        "select cat.nome, count(*) from chamados c join categorias cat on cat.id=c.categoria_id where c.criado_em >= ? and c.criado_em < ? group by cat.nome order by cat.nome",
        (ResultSet rs) -> {
          categorias.put(rs.getString(1), rs.getLong(2));
        },
        inicio,
        fim);
    return new DashboardResponse(
        indicadores.total(),
        indicadores.abertos(),
        indicadores.vencidos(),
        indicadores.vencendo(),
        indicadores.media(),
        contagens("status", inicio, fim),
        contagens("prioridade", inicio, fim),
        categorias,
        indicadores.resolvidos() == 0
            ? null
            : 100.0 * indicadores.cumpridos() / indicadores.resolvidos(),
        indicadores.resolvidos(),
        indicadores.semTempo(),
        desde,
        ate);
  }

  private record Agregado(
      long total,
      long abertos,
      long vencidos,
      long vencendo,
      Double media,
      long resolvidos,
      long cumpridos,
      long semTempo) {}

  private Map<String, Long> contagens(String coluna, Timestamp inicio, Timestamp fim) {
    Map<String, Long> resultado = new LinkedHashMap<>();
    jdbc.query(
        "select "
            + coluna
            + ", count(*) from chamados where criado_em >= ? and criado_em < ? group by "
            + coluna,
        (ResultSet rs) -> {
          resultado.put(rs.getString(1), rs.getLong(2));
        },
        inicio,
        fim);
    return resultado;
  }
}
