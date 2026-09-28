package br.com.empresa.helpdesk.sla;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.empresa.helpdesk.admin.domain.Categoria;
import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import br.com.empresa.helpdesk.chamados.infra.ChamadoRepository;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SlaAlertasConsultaTest {
  @Autowired JdbcTemplate jdbc;
  @Autowired UsuarioRepository usuarios;
  @Autowired CategoriaRepository categorias;
  @Autowired ChamadoRepository chamados;

  @Test
  void incluiSemResponsavelMasIgnoraPrimeiraRespostaJaFeitaPausaEConclusao() {
    var usuario =
        usuarios.saveAndFlush(
            Usuario.novoFuncionario("Pessoa fictícia", "alerta-consulta@example.com"));
    var categoria = categorias.saveAndFlush(new Categoria("Consulta SLA C3"));
    Instant agora = Instant.parse("2030-01-07T12:00:00Z");
    for (int i = 1; i <= 4; i++) {
      jdbc.update(
          "insert into chamados (numero,titulo,descricao,solicitante_id,categoria_id,prioridade,status,canal,criado_em,atualizado_em,prazo_primeira_resposta,prazo_resolucao,primeira_resposta_em,sla_pausado_em,version) values (?,?,?,?,?,'MEDIA',?,'PORTAL',?,?,?,?,?,?,0)",
          "CH-2030-C3-" + i,
          "Falha de rede",
          "Descrição fictícia",
          usuario.getId(),
          categoria.getId(),
          i == 4 ? "RESOLVIDO" : "ABERTO",
          Timestamp.from(agora),
          Timestamp.from(agora),
          Timestamp.from(agora.plusSeconds(1800)),
          Timestamp.from(agora.plusSeconds(1800)),
          i == 2 ? Timestamp.from(agora) : null,
          i == 3 ? Timestamp.from(agora) : null);
    }
    assertThat(chamados.vencendoPrimeiraResposta(agora, agora.plusSeconds(3600)))
        .extracting(c -> c.getNumero())
        .containsExactly("CH-2030-C3-1");
    assertThat(chamados.vencendoSla(agora, agora.plusSeconds(3600)))
        .extracting(c -> c.getNumero())
        .containsExactlyInAnyOrder("CH-2030-C3-1", "CH-2030-C3-2");
  }
}
