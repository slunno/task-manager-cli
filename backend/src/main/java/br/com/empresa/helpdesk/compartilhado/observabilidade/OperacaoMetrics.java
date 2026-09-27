package br.com.empresa.helpdesk.compartilhado.observabilidade;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class OperacaoMetrics {
  @Bean
  MeterBinder helpdeskMeters(JdbcTemplate jdbc, Clock clock) {
    return registry -> {
      Gauge.builder(
              "helpdesk.outbox.pending",
              jdbc,
              db ->
                  contar(
                      db,
                      "select count(*) from notificacoes_outbox where status in ('PENDENTE','FALHA')"))
          .description("Mensagens aguardando envio ou nova tentativa")
          .register(registry);
      Gauge.builder(
              "helpdesk.sla.overdue",
              jdbc,
              db ->
                  contar(
                      db,
                      "select count(*) from chamados where status not in ('RESOLVIDO','FECHADO') and sla_pausado_em is null and prazo_resolucao < ?",
                      Timestamp.from(clock.instant())))
          .description("Chamados ativos com SLA de resolução vencido")
          .register(registry);
      Gauge.builder(
              "helpdesk.retention.storage.pending",
              jdbc,
              db -> contar(db, "select count(*) from storage_exclusao_pendente"))
          .description("Anexos aguardando remoção física pela retenção")
          .register(registry);
    };
  }

  private double contar(JdbcTemplate jdbc, String sql, Object... argumentos) {
    Long valor = jdbc.queryForObject(sql, Long.class, argumentos);
    return valor == null ? 0 : valor.doubleValue();
  }
}
