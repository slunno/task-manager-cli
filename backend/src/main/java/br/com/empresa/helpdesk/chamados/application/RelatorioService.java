package br.com.empresa.helpdesk.chamados.application;

import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatorioService {
  private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
  private final JdbcTemplate jdbc;

  public RelatorioService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record Linha(
      Long setorId,
      String setor,
      Long categoriaId,
      String categoria,
      long total,
      long resolvidos,
      Double mediaResolucaoHoras) {}

  public record Relatorio(LocalDate desde, LocalDate ate, List<Linha> linhas) {}

  @Transactional(readOnly = true)
  public Relatorio consultar(LocalDate desde, LocalDate ate, Long setorId, Long categoriaId) {
    if (desde == null
        || ate == null
        || ate.isBefore(desde)
        || ChronoUnit.DAYS.between(desde, ate) > 366)
      throw new RequisicaoInvalidaException("Informe período válido de até 366 dias");
    if ((setorId != null && setorId <= 0) || (categoriaId != null && categoriaId <= 0))
      throw new RequisicaoInvalidaException("Filtro de setor ou categoria inválido");
    StringBuilder sql =
        new StringBuilder(
            "select s.id, coalesce(s.nome, 'Sem setor'), cat.id, cat.nome, count(*), sum(case when c.status in ('RESOLVIDO','FECHADO') then 1 else 0 end), avg(case when c.resolvido_em is not null then extract(epoch from (c.resolvido_em - c.criado_em)) / 3600 else null end) from chamados c join usuarios u on u.id = c.solicitante_id left join setores s on s.id = u.setor_id join categorias cat on cat.id = c.categoria_id where c.criado_em >= ? and c.criado_em < ?");
    var inicio = Timestamp.from(desde.atStartOfDay(ZONA).toInstant());
    var fim = Timestamp.from(ate.plusDays(1).atStartOfDay(ZONA).toInstant());
    List<Object> parametros = new ArrayList<>(List.of(inicio, fim));
    if (setorId != null) {
      sql.append(" and s.id = ?");
      parametros.add(setorId);
    }
    if (categoriaId != null) {
      sql.append(" and cat.id = ?");
      parametros.add(categoriaId);
    }
    sql.append(" group by s.id, s.nome, cat.id, cat.nome order by s.nome, cat.nome");
    List<Linha> linhas =
        jdbc.query(
            sql.toString(),
            (rs, i) ->
                new Linha(
                    rs.getObject(1, Long.class),
                    rs.getString(2),
                    rs.getObject(3, Long.class),
                    rs.getString(4),
                    rs.getLong(5),
                    rs.getLong(6),
                    rs.getObject(7) == null ? null : ((Number) rs.getObject(7)).doubleValue()),
            parametros.toArray());
    return new Relatorio(desde, ate, linhas);
  }

  public byte[] csv(Relatorio relatorio) {
    StringBuilder csv =
        new StringBuilder("desde;ate;setor;categoria;total;resolvidos;media_resolucao_horas\r\n");
    for (Linha linha : relatorio.linhas()) {
      csv.append(relatorio.desde())
          .append(';')
          .append(relatorio.ate())
          .append(';')
          .append(campoCsv(linha.setor()))
          .append(';')
          .append(campoCsv(linha.categoria()))
          .append(';')
          .append(linha.total())
          .append(';')
          .append(linha.resolvidos())
          .append(';')
          .append(
              linha.mediaResolucaoHoras() == null
                  ? ""
                  : String.format(java.util.Locale.ROOT, "%.2f", linha.mediaResolucaoHoras()))
          .append("\r\n");
    }
    return csv.toString().getBytes(StandardCharsets.UTF_8);
  }

  private String campoCsv(String valor) {
    String seguro = valor.matches("^[=+@-].*") ? "'" + valor : valor;
    return "\"" + seguro.replace("\"", "\"\"") + "\"";
  }
}
