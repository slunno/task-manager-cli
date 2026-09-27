package br.com.empresa.helpdesk.sla;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.helpdesk.chamados.domain.Chamado;
import br.com.empresa.helpdesk.sla.application.SlaService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class SlaPausaTest {
  @Test
  void prazoJaVencidoContinuaVencidoAoRetomar() {
    var fonte = new DriverManagerDataSource("jdbc:h2:mem:sla-pausa;DB_CLOSE_DELAY=-1", "sa", "");
    var jdbc = new JdbcTemplate(fonte);
    jdbc.execute(
        "create table if not exists sla_politicas (prioridade varchar(10) primary key, horas_primeira_resposta int, horas_resolucao int)");
    jdbc.execute(
        "create table if not exists expediente (dia_semana int, inicio time, fim time, ativo boolean)");
    jdbc.execute("create table if not exists feriados (data date)");
    jdbc.update("delete from sla_politicas");
    jdbc.update("delete from expediente");
    jdbc.update("insert into sla_politicas values ('MEDIA', 1, 2)");
    jdbc.update("insert into expediente values (1, '09:00', '18:00', true)");
    var sla = new SlaService(jdbc);
    Instant abertura = Instant.parse("2026-09-21T12:00:00Z");
    var chamado =
        Chamado.abrir("CH-2026-000001", "Acesso", "Falha de acesso", 1L, null, 1L, null, abertura);
    sla.iniciar(chamado, abertura);
    chamado.pausarSla(abertura.plusSeconds(3 * 3600));
    Instant retomada = abertura.plusSeconds(5 * 3600);
    sla.retomar(chamado, retomada);
    assertThat(chamado.getPrazoResolucao()).isBefore(retomada);
    assertThat(chamado.getSlaPausadoEm()).isNull();
  }
}
