package br.com.empresa.helpdesk.sla.application;

import br.com.empresa.helpdesk.chamados.domain.Prioridade;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SlaAdminService {
  private final JdbcTemplate jdbc;

  public SlaAdminService(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional(readOnly = true)
  public List<Politica> politicas() {
    return jdbc.query(
        "select prioridade, horas_primeira_resposta, horas_resolucao from sla_politicas order by prioridade",
        (rs, i) -> new Politica(Prioridade.valueOf(rs.getString(1)), rs.getInt(2), rs.getInt(3)));
  }

  @Transactional
  public Politica atualizarPolitica(Prioridade prioridade, int primeiraResposta, int resolucao) {
    if (primeiraResposta < 1 || resolucao < primeiraResposta || resolucao > 1000)
      throw new RequisicaoInvalidaException(
          "Horas de SLA inválidas: resolução deve ser maior ou igual à primeira resposta");
    int afetados =
        jdbc.update(
            "update sla_politicas set horas_primeira_resposta = ?, horas_resolucao = ?, atualizado_em = current_timestamp where prioridade = ?",
            primeiraResposta,
            resolucao,
            prioridade.name());
    if (afetados == 0) throw new RequisicaoInvalidaException("Política de SLA inexistente");
    return new Politica(prioridade, primeiraResposta, resolucao);
  }

  @Transactional(readOnly = true)
  public Calendario calendario() {
    List<Janela> janelas =
        jdbc.query(
            "select dia_semana, inicio, fim from expediente where ativo = true order by dia_semana, inicio",
            (rs, i) ->
                new Janela(rs.getInt(1), rs.getTime(2).toLocalTime(), rs.getTime(3).toLocalTime()));
    List<Feriado> feriados =
        jdbc.query(
            "select id, data, descricao from feriados order by data",
            (rs, i) -> new Feriado(rs.getLong(1), rs.getDate(2).toLocalDate(), rs.getString(3)));
    return new Calendario(janelas, feriados);
  }

  @Transactional
  public Calendario atualizarExpediente(List<Janela> janelas) {
    if (janelas == null || janelas.isEmpty() || janelas.size() > 21)
      throw new RequisicaoInvalidaException("Informe de 1 a 21 janelas de expediente");
    List<Janela> ordenadas = new ArrayList<>(janelas);
    ordenadas.sort(
        (a, b) -> {
          int dia = Integer.compare(a.diaSemana(), b.diaSemana());
          return dia == 0 ? a.inicio().compareTo(b.inicio()) : dia;
        });
    for (int i = 0; i < ordenadas.size(); i++) {
      Janela atual = ordenadas.get(i);
      if (atual.diaSemana() < 1
          || atual.diaSemana() > 7
          || atual.inicio() == null
          || atual.fim() == null
          || !atual.inicio().isBefore(atual.fim()))
        throw new RequisicaoInvalidaException("Janela de expediente inválida");
      if (i > 0) {
        Janela anterior = ordenadas.get(i - 1);
        if (anterior.diaSemana() == atual.diaSemana() && anterior.fim().isAfter(atual.inicio()))
          throw new RequisicaoInvalidaException("Janelas de expediente sobrepostas");
      }
    }
    jdbc.update("delete from expediente");
    for (Janela janela : ordenadas)
      jdbc.update(
          "insert into expediente (dia_semana, inicio, fim) values (?, ?, ?)",
          janela.diaSemana(),
          janela.inicio(),
          janela.fim());
    return calendario();
  }

  @Transactional
  public Feriado adicionarFeriado(LocalDate data, String descricao) {
    if (data == null || descricao == null || descricao.isBlank() || descricao.trim().length() > 180)
      throw new RequisicaoInvalidaException("Data ou descrição de feriado inválida");
    Integer existe =
        jdbc.queryForObject("select count(*) from feriados where data = ?", Integer.class, data);
    if (existe != null && existe > 0)
      throw new RequisicaoInvalidaException("Feriado já cadastrado");
    jdbc.update("insert into feriados (data, descricao) values (?, ?)", data, descricao.trim());
    return jdbc.queryForObject(
        "select id, data, descricao from feriados where data = ?",
        (rs, i) -> new Feriado(rs.getLong(1), rs.getDate(2).toLocalDate(), rs.getString(3)),
        data);
  }

  @Transactional
  public void removerFeriado(long id) {
    if (jdbc.update("delete from feriados where id = ?", id) == 0)
      throw new RequisicaoInvalidaException("Feriado não encontrado");
  }

  public record Politica(Prioridade prioridade, int horasPrimeiraResposta, int horasResolucao) {}

  public record Janela(int diaSemana, LocalTime inicio, LocalTime fim) {}

  public record Feriado(long id, LocalDate data, String descricao) {}

  public record Calendario(List<Janela> expediente, List<Feriado> feriados) {}
}
