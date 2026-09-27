package br.com.empresa.helpdesk.sla.application;

import br.com.empresa.helpdesk.chamados.domain.Chamado;
import br.com.empresa.helpdesk.sla.domain.CalendarioUtil;
import br.com.empresa.helpdesk.sla.domain.CalendarioUtil.Janela;
import java.time.*;
import java.util.HashSet;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class SlaService {
  private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
  private final JdbcTemplate jdbc;

  public SlaService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void iniciar(Chamado chamado, Instant agora) {
    int[] politica =
        jdbc.queryForObject(
            "select horas_primeira_resposta, horas_resolucao from sla_politicas where prioridade = ?",
            (rs, linha) -> new int[] {rs.getInt(1), rs.getInt(2)},
            chamado.getPrioridade().name());
    var calendario = calendario();
    long resposta = politica[0];
    long resolucao = politica[1];
    chamado.definirPrazos(
        calendario.adicionarMinutosUteis(agora, resposta * 60),
        calendario.adicionarMinutosUteis(agora, resolucao * 60));
  }

  public void retomar(Chamado chamado, Instant agora) {
    if (chamado.getSlaPausadoEm() == null) return;
    var calendario = calendario();
    long respostaRestante =
        chamado.getPrazoPrimeiraResposta() == null
            ? 0
            : calendario.minutosUteisEntre(
                chamado.getSlaPausadoEm(), chamado.getPrazoPrimeiraResposta());
    long resolucaoRestante =
        calendario.minutosUteisEntre(chamado.getSlaPausadoEm(), chamado.getPrazoResolucao());
    chamado.retomarSla(
        chamado.getPrazoPrimeiraResposta() == null
            ? null
            : calendario.adicionarMinutosUteis(agora, respostaRestante),
        calendario.adicionarMinutosUteis(agora, resolucaoRestante));
  }

  public CalendarioUtil calendario() {
    var janelas =
        jdbc.query(
            "select dia_semana, inicio, fim from expediente where ativo = true order by dia_semana, inicio",
            (rs, i) ->
                new Janela(
                    DayOfWeek.of(rs.getInt(1)),
                    rs.getTime(2).toLocalTime(),
                    rs.getTime(3).toLocalTime()));
    Set<LocalDate> feriados =
        new HashSet<>(
            jdbc.query("select data from feriados", (rs, i) -> rs.getDate(1).toLocalDate()));
    return new CalendarioUtil(ZONA, janelas, feriados);
  }
}
