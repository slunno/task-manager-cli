package br.com.empresa.helpdesk.avaliacoes;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.empresa.helpdesk.admin.domain.Categoria;
import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class ConclusaoIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired CategoriaRepository categorias;
  @Autowired UsuarioRepository usuarios;
  @Autowired ChamadoService chamados;
  @Autowired JdbcTemplate jdbc;

  @Test
  void avaliarReabrirEAvaliarNovamente() throws Exception {
    String sufixo = Long.toString(System.nanoTime());
    MockHttpSession solicitante = entrar("e8-solicitante-" + sufixo + "@empresa.com");
    String emailAgente = "e8-agente-" + sufixo + "@empresa.com";
    var agenteUsuario =
        usuarios.saveAndFlush(
            br.com.empresa.helpdesk.usuarios.domain.Usuario.novoFuncionario(
                "Agente E8", emailAgente));
    agenteUsuario.alterarPerfil(Perfil.TI_AGENTE);
    usuarios.saveAndFlush(agenteUsuario);
    MockHttpSession agente = entrar(emailAgente);
    Long categoriaId = categorias.saveAndFlush(new Categoria("E8 " + sufixo)).getId();
    JsonNode criado =
        json(
            mvc.perform(
                    post("/api/v1/chamados")
                        .session(solicitante)
                        .header("X-CSRF-TOKEN", csrf(solicitante))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            mapper.writeValueAsString(
                                Map.of(
                                    "titulo",
                                    "Reabertura e avaliação",
                                    "descricao",
                                    "Notebook sem acesso à rede corporativa",
                                    "categoriaId",
                                    categoriaId))))
                .andExpect(status().isCreated())
                .andReturn());
    long id = criado.path("id").asLong();
    mvc.perform(
            post("/api/v1/chamados/" + id + "/avaliacao")
                .session(solicitante)
                .header("X-CSRF-TOKEN", csrf(solicitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nota\":5}"))
        .andExpect(status().isConflict());
    JsonNode assumido =
        json(
            mvc.perform(
                    post("/api/v1/chamados/" + id + "/assumir")
                        .session(agente)
                        .header("X-CSRF-TOKEN", csrf(agente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + criado.path("version").asLong() + "}"))
                .andExpect(status().isOk())
                .andReturn());
    JsonNode resolvido =
        json(
            mvc.perform(
                    patch("/api/v1/chamados/" + id)
                        .session(agente)
                        .header("X-CSRF-TOKEN", csrf(agente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"version\":"
                                + assumido.path("version").asLong()
                                + ",\"status\":\"RESOLVIDO\",\"solucao\":\"Acesso restabelecido\"}"))
                .andExpect(status().isOk())
                .andReturn());
    mvc.perform(
            post("/api/v1/chamados/" + id + "/avaliacao")
                .session(agente)
                .header("X-CSRF-TOKEN", csrf(agente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nota\":5}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/chamados/" + id + "/avaliacao")
                .session(solicitante)
                .header("X-CSRF-TOKEN", csrf(solicitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nota\":5,\"comentario\":\"Resolvido rapidamente\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/chamados/" + id + "/avaliacao")
                .session(solicitante)
                .header("X-CSRF-TOKEN", csrf(solicitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nota\":4}"))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/v1/chamados/" + id + "/reabertura")
                .session(solicitante)
                .header("X-CSRF-TOKEN", csrf(solicitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + assumido.path("version").asLong() + "}"))
        .andExpect(status().isConflict());
    mvc.perform(
            post("/api/v1/chamados/" + id + "/reabertura")
                .session(solicitante)
                .header("X-CSRF-TOKEN", csrf(solicitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + resolvido.path("version").asLong() + "}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ABERTO"))
        .andExpect(jsonPath("$.responsavelId").isEmpty());
    mvc.perform(get("/api/v1/chamados/" + id + "/avaliacao").session(solicitante))
        .andExpect(status().isNotFound());
  }

  @Test
  void fechamentoAutomaticoELimiteDeReabertura() throws Exception {
    String sufixo = Long.toString(System.nanoTime());
    MockHttpSession solicitante = entrar("e8-fechar-" + sufixo + "@empresa.com");
    String emailAgente = "e8-fechar-agente-" + sufixo + "@empresa.com";
    var agenteUsuario =
        usuarios.saveAndFlush(
            br.com.empresa.helpdesk.usuarios.domain.Usuario.novoFuncionario(
                "Agente E8", emailAgente));
    agenteUsuario.alterarPerfil(Perfil.TI_AGENTE);
    usuarios.saveAndFlush(agenteUsuario);
    MockHttpSession agente = entrar(emailAgente);
    Long categoriaId = categorias.saveAndFlush(new Categoria("E8 fechar " + sufixo)).getId();
    JsonNode criado =
        json(
            mvc.perform(
                    post("/api/v1/chamados")
                        .session(solicitante)
                        .header("X-CSRF-TOKEN", csrf(solicitante))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            mapper.writeValueAsString(
                                Map.of(
                                    "titulo",
                                    "Fechamento automático",
                                    "descricao",
                                    "Problema de acesso resolvido em teste",
                                    "categoriaId",
                                    categoriaId))))
                .andExpect(status().isCreated())
                .andReturn());
    long id = criado.path("id").asLong();
    JsonNode assumido =
        json(
            mvc.perform(
                    post("/api/v1/chamados/" + id + "/assumir")
                        .session(agente)
                        .header("X-CSRF-TOKEN", csrf(agente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + criado.path("version").asLong() + "}"))
                .andExpect(status().isOk())
                .andReturn());
    JsonNode resolvido =
        json(
            mvc.perform(
                    patch("/api/v1/chamados/" + id)
                        .session(agente)
                        .header("X-CSRF-TOKEN", csrf(agente))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            "{\"version\":"
                                + assumido.path("version").asLong()
                                + ",\"status\":\"RESOLVIDO\",\"solucao\":\"Pronto\"}"))
                .andExpect(status().isOk())
                .andReturn());
    jdbc.update(
        "update chamados set resolvido_em = ? where id = ?",
        Instant.now().minus(8, ChronoUnit.DAYS),
        id);
    mvc.perform(
            post("/api/v1/chamados/" + id + "/reabertura")
                .session(solicitante)
                .header("X-CSRF-TOKEN", csrf(solicitante))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":" + resolvido.path("version").asLong() + "}"))
        .andExpect(status().isConflict());
    org.assertj.core.api.Assertions.assertThat(chamados.fecharAntigos()).isGreaterThanOrEqualTo(1);
    mvc.perform(get("/api/v1/chamados/" + id).session(solicitante))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("FECHADO"))
        .andExpect(jsonPath("$.fechadoEm").isNotEmpty());
  }

  private MockHttpSession entrar(String email) throws Exception {
    var token = mvc.perform(get("/api/v1/auth/csrf")).andReturn();
    MockHttpSession sessao = (MockHttpSession) token.getRequest().getSession(false);
    mvc.perform(
            post("/api/v1/auth/dev/login")
                .session(sessao)
                .header("X-CSRF-TOKEN", json(token).path("token").asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", email, "nome", "Teste E8"))))
        .andExpect(status().isOk());
    return sessao;
  }

  private String csrf(MockHttpSession sessao) throws Exception {
    return json(mvc.perform(get("/api/v1/auth/csrf").session(sessao)).andReturn())
        .path("token")
        .asText();
  }

  private JsonNode json(org.springframework.test.web.servlet.MvcResult resultado) throws Exception {
    return mapper.readTree(resultado.getResponse().getContentAsString());
  }
}
