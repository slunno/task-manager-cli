package br.com.empresa.helpdesk.gestao;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.empresa.helpdesk.admin.domain.Categoria;
import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class GestaoIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UsuarioRepository usuarios;
  @Autowired CategoriaRepository categorias;

  @Test
  void avisosDuplicidadeERelatorioRespeitamPerfis() throws Exception {
    MockHttpSession funcionario = entrar("e10-funcionario@empresa.com");
    MockHttpSession agente = entrarTi("e10-agente@empresa.com", Perfil.TI_AGENTE);
    MockHttpSession admin = entrarTi("e10-admin@empresa.com", Perfil.TI_ADMIN);
    mvc.perform(get("/actuator/metrics").session(funcionario)).andExpect(status().isForbidden());
    mvc.perform(get("/actuator/metrics").session(admin)).andExpect(status().isOk());
    mvc.perform(get("/api/v1/ti/admin/setores").session(agente)).andExpect(status().isForbidden());
    String setorResposta =
        mvc.perform(
                post("/api/v1/ti/admin/setores")
                    .session(admin)
                    .header("X-CSRF-TOKEN", csrf(admin))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Operações E11\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long setorId = mapper.readTree(setorResposta).path("id").asLong();
    mvc.perform(get("/api/v1/ti/setores").session(agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.id == " + setorId + ")]", org.hamcrest.Matchers.hasSize(1)));
    Long funcionarioId =
        usuarios.findByEmailIgnoreCase("e10-funcionario@empresa.com").orElseThrow().getId();
    String alteracaoUsuario =
        mapper.writeValueAsString(
            Map.of(
                "nome",
                "Teste",
                "email",
                "e10-funcionario@empresa.com",
                "perfil",
                "FUNCIONARIO",
                "ativo",
                true,
                "setorId",
                setorId));
    mvc.perform(
            patch("/api/v1/ti/admin/usuarios/" + funcionarioId)
                .session(agente)
                .header("X-CSRF-TOKEN", csrf(agente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(alteracaoUsuario))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/v1/ti/admin/usuarios/" + funcionarioId)
                .session(admin)
                .header("X-CSRF-TOKEN", csrf(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(alteracaoUsuario))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.setorId").value(setorId));
    String aviso =
        mapper.writeValueAsString(
            Map.of(
                "titulo",
                "Falha de rede",
                "mensagem",
                "A equipe está investigando a indisponibilidade.",
                "inicioEm",
                Instant.now().minusSeconds(60).toString(),
                "ativo",
                true));
    mvc.perform(
            post("/api/v1/ti/admin/avisos")
                .session(agente)
                .header("X-CSRF-TOKEN", csrf(agente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aviso))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/ti/admin/avisos")
                .session(admin)
                .header("X-CSRF-TOKEN", csrf(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(aviso))
        .andExpect(status().isCreated());
    mvc.perform(get("/api/v1/avisos").session(funcionario))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[?(@.titulo == 'Falha de rede')]", org.hamcrest.Matchers.hasSize(1)));

    long categoriaId =
        categorias.findByAtivaTrueOrderByNomeAsc().stream()
            .findFirst()
            .orElseGet(() -> categorias.saveAndFlush(new Categoria("Categoria E10")))
            .getId();
    long primeiro = criarChamado(funcionario, "Problema de rede no andar 1", categoriaId);
    long segundo = criarChamado(funcionario, "Problema de rede no andar 2", categoriaId);
    mvc.perform(get("/api/v1/ti/chamados/" + segundo + "/duplicidade").session(funcionario))
        .andExpect(status().isForbidden());
    mvc.perform(
            put("/api/v1/ti/chamados/" + segundo + "/duplicidade")
                .session(agente)
                .header("X-CSRF-TOKEN", csrf(agente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"principalId\":" + primeiro + ",\"version\":0}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.principalId").value(primeiro));
    mvc.perform(get("/api/v1/ti/chamados/" + primeiro + "/duplicidade").session(agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.duplicados[0]").value(segundo));
    mvc.perform(
            put("/api/v1/ti/chamados/" + primeiro + "/duplicidade")
                .session(agente)
                .header("X-CSRF-TOKEN", csrf(agente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"principalId\":" + segundo + ",\"version\":0}"))
        .andExpect(status().isBadRequest());

    LocalDate hoje = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo"));
    String caminho = "/api/v1/ti/relatorios?desde=" + hoje.minusDays(1) + "&ate=" + hoje;
    mvc.perform(get(caminho).session(funcionario)).andExpect(status().isForbidden());
    mvc.perform(get(caminho).session(agente))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$.linhas[0].total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    mvc.perform(get(caminho + "&setorId=" + setorId).session(agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.linhas[0].setor").value("Operações E11"));
    mvc.perform(
            get("/api/v1/ti/relatorios/exportacao.csv?desde=" + hoje.minusDays(1) + "&ate=" + hoje)
                .session(agente))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=relatorio-chamados.csv"))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("desde;ate;setor;categoria")));
  }

  private long criarChamado(MockHttpSession sessao, String titulo, long categoriaId)
      throws Exception {
    String corpo =
        mapper.writeValueAsString(
            Map.of(
                "titulo",
                titulo,
                "descricao",
                "A conexão falhou durante toda a manhã.",
                "categoriaId",
                categoriaId));
    String resposta =
        mvc.perform(
                post("/api/v1/chamados")
                    .session(sessao)
                    .header("X-CSRF-TOKEN", csrf(sessao))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return mapper.readTree(resposta).path("id").asLong();
  }

  private MockHttpSession entrarTi(String email, Perfil perfil) throws Exception {
    Usuario pessoa =
        usuarios
            .findByEmailIgnoreCase(email)
            .orElseGet(() -> Usuario.novoFuncionario("Equipe E10", email));
    pessoa.alterarPerfil(perfil);
    usuarios.saveAndFlush(pessoa);
    return entrar(email);
  }

  private MockHttpSession entrar(String email) throws Exception {
    var token = mvc.perform(get("/api/v1/auth/csrf")).andReturn();
    MockHttpSession sessao = (MockHttpSession) token.getRequest().getSession(false);
    mvc.perform(
            post("/api/v1/auth/dev/login")
                .session(sessao)
                .header(
                    "X-CSRF-TOKEN",
                    mapper
                        .readTree(token.getResponse().getContentAsString())
                        .path("token")
                        .asText())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("email", email, "nome", "Teste"))))
        .andExpect(status().isOk());
    return sessao;
  }

  private String csrf(MockHttpSession sessao) throws Exception {
    return mapper
        .readTree(
            mvc.perform(get("/api/v1/auth/csrf").session(sessao))
                .andReturn()
                .getResponse()
                .getContentAsString())
        .path("token")
        .asText();
  }
}
