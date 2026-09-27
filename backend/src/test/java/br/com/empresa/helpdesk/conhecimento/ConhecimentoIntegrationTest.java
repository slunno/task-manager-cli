package br.com.empresa.helpdesk.conhecimento;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ConhecimentoIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UsuarioRepository usuarios;

  @Test
  void artigoRascunhoNaoVazaEFiltroPertenceAoAgente() throws Exception {
    MockHttpSession funcionario = entrar("e9-funcionario@empresa.com");
    MockHttpSession agente = entrarTi("e9-agente@empresa.com");
    MockHttpSession outroAgente = entrarTi("e9-outro@empresa.com");

    mvc.perform(
            post("/api/v1/ti/artigos")
                .session(funcionario)
                .header("X-CSRF-TOKEN", csrf(funcionario))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"titulo\":\"Artigo restrito\",\"conteudo\":\"Conteúdo suficientemente longo para o artigo\",\"publicado\":true}"))
        .andExpect(status().isForbidden());
    String artigo =
        mvc.perform(
                post("/api/v1/ti/artigos")
                    .session(agente)
                    .header("X-CSRF-TOKEN", csrf(agente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"titulo\":\"Artigo restrito\",\"conteudo\":\"Conteúdo suficientemente longo para o artigo\",\"publicado\":false}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long artigoId = mapper.readTree(artigo).path("id").asLong();
    mvc.perform(get("/api/v1/artigos/" + artigoId).session(funcionario))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/artigos").session(funcionario).param("texto", "Artigo restrito"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
    mvc.perform(
            put("/api/v1/ti/artigos/" + artigoId)
                .session(agente)
                .header("X-CSRF-TOKEN", csrf(agente))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"titulo\":\"Artigo restrito\",\"conteudo\":\"Conteúdo suficientemente longo para o artigo\",\"publicado\":true}"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/v1/artigos/" + artigoId).session(funcionario)).andExpect(status().isOk());

    String filtro =
        mvc.perform(
                post("/api/v1/ti/filtros-salvos")
                    .session(agente)
                    .header("X-CSRF-TOKEN", csrf(agente))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nome\":\"Urgentes\",\"parametros\":\"prioridade=ALTA\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long filtroId = mapper.readTree(filtro).path("id").asLong();
    mvc.perform(get("/api/v1/ti/filtros-salvos").session(outroAgente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    mvc.perform(
            delete("/api/v1/ti/filtros-salvos/" + filtroId)
                .session(outroAgente)
                .header("X-CSRF-TOKEN", csrf(outroAgente)))
        .andExpect(status().isNotFound());
    mvc.perform(
            post("/api/v1/ti/filtros-salvos")
                .session(agente)
                .header("X-CSRF-TOKEN", csrf(agente))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Invalido\",\"parametros\":\"admin=true\"}"))
        .andExpect(status().isBadRequest());
  }

  private MockHttpSession entrarTi(String email) throws Exception {
    Usuario agente =
        usuarios
            .findByEmailIgnoreCase(email)
            .orElseGet(() -> Usuario.novoFuncionario("Agente E9", email));
    agente.alterarPerfil(Perfil.TI_AGENTE);
    usuarios.saveAndFlush(agente);
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
                .content(
                    mapper.writeValueAsString(java.util.Map.of("email", email, "nome", "Teste"))))
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
