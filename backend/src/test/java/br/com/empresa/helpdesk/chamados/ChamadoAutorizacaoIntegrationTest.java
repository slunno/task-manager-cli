package br.com.empresa.helpdesk.chamados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.empresa.helpdesk.admin.domain.Categoria;
import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import br.com.empresa.helpdesk.chamados.infra.ChamadoRepository;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class ChamadoAutorizacaoIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private CategoriaRepository categorias;
  @Autowired private ChamadoRepository chamados;
  @Autowired private UsuarioRepository usuarios;

  private Long categoriaId;

  @BeforeEach
  void preparar() {
    chamados.deleteAll();
    categorias.deleteAll();
    usuarios.deleteAll();
    categoriaId = categorias.saveAndFlush(new Categoria("Equipamentos")).getId();
  }

  @Test
  void anonimoNaoAcessaRecursos() throws Exception {
    mvc.perform(get("/api/v1/categorias")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/chamados/meus")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/chamados/1")).andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/chamados")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Teste", null)))
        .andExpect(status().isForbidden());
    MvcResult token = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
    mvc.perform(
            post("/api/v1/chamados")
                .session((MockHttpSession) token.getRequest().getSession(false))
                .header("X-CSRF-TOKEN", valorToken(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Teste", null)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void funcionarioVeSomenteChamadosPropriosEAlheioRetorna404() throws Exception {
    MockHttpSession maria = entrar("maria@empresa.com", "Maria");
    MockHttpSession joao = entrar("joao@empresa.com", "João");
    long chamadoMaria = criar(maria, "Acesso bloqueado", null).path("id").asLong();
    long chamadoJoao = criar(joao, "Notebook não liga", null).path("id").asLong();

    mvc.perform(get("/api/v1/chamados/meus").session(maria))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(chamadoMaria));
    mvc.perform(get("/api/v1/chamados/" + chamadoMaria).session(maria)).andExpect(status().isOk());
    mvc.perform(get("/api/v1/chamados/" + chamadoJoao).session(maria))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/chamados/999999").session(maria)).andExpect(status().isNotFound());
  }

  @Test
  void agenteEAdministradorVeemChamadosDeTerceiros() throws Exception {
    MockHttpSession maria = entrar("maria@empresa.com", "Maria");
    MockHttpSession agente = entrar("agente@empresa.com", "Agente");
    MockHttpSession admin = entrar("admin@empresa.com", "Admin");
    long id = criar(maria, "Acesso bloqueado", null).path("id").asLong();
    var usuarioAgente = usuarios.findByEmailIgnoreCase("agente@empresa.com").orElseThrow();
    usuarioAgente.alterarPerfil(Perfil.TI_AGENTE);
    usuarios.saveAndFlush(usuarioAgente);
    var usuarioAdmin = usuarios.findByEmailIgnoreCase("admin@empresa.com").orElseThrow();
    usuarioAdmin.alterarPerfil(Perfil.TI_ADMIN);
    usuarios.saveAndFlush(usuarioAdmin);

    mvc.perform(get("/api/v1/chamados/" + id).session(agente)).andExpect(status().isOk());
    mvc.perform(get("/api/v1/chamados/" + id).session(admin)).andExpect(status().isOk());
    long mariaId = usuarios.findByEmailIgnoreCase("maria@empresa.com").orElseThrow().getId();
    JsonNode criadoPeloAdmin = criar(admin, "Acesso remoto", mariaId);
    assertThat(criadoPeloAdmin.path("solicitanteId").asLong()).isEqualTo(mariaId);
    mvc.perform(get("/api/v1/chamados/meus").session(agente))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void solicitanteEhDefinidoPelaSessaoEAgentePodeAbrirEmNomeDeOutro() throws Exception {
    MockHttpSession maria = entrar("maria@empresa.com", "Maria");
    MockHttpSession agente = entrar("agente@empresa.com", "Agente");
    long mariaId = usuarios.findByEmailIgnoreCase("maria@empresa.com").orElseThrow().getId();
    long agenteId = usuarios.findByEmailIgnoreCase("agente@empresa.com").orElseThrow().getId();
    var usuarioAgente = usuarios.findByEmailIgnoreCase("agente@empresa.com").orElseThrow();
    usuarioAgente.alterarPerfil(Perfil.TI_AGENTE);
    usuarios.saveAndFlush(usuarioAgente);

    mvc.perform(
            post("/api/v1/chamados")
                .session(maria)
                .header("X-CSRF-TOKEN", csrf(maria))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("Tentativa", mariaId)))
        .andExpect(status().isBadRequest());
    assertThat(chamados.count()).isZero();

    JsonNode criado = criar(agente, "Impressora parada", mariaId);
    assertThat(criado.path("solicitanteId").asLong()).isEqualTo(mariaId);
    assertThat(criado.path("abertoPorId").asLong()).isEqualTo(agenteId);
    mvc.perform(get("/api/v1/chamados/" + criado.path("id").asLong()).session(maria))
        .andExpect(status().isOk());
  }

  @Test
  void validaEntradaPaginacaoENumeroLegivel() throws Exception {
    MockHttpSession maria = entrar("maria@empresa.com", "Maria");
    mvc.perform(get("/api/v1/categorias").session(maria))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nome").value("Equipamentos"));
    mvc.perform(
            post("/api/v1/chamados")
                .session(maria)
                .header("X-CSRF-TOKEN", csrf(maria))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(" ", null)))
        .andExpect(status().isBadRequest());
    JsonNode primeiro = criar(maria, "Notebook não liga", null);
    JsonNode segundo = criar(maria, "VPN indisponível", null);
    assertThat(primeiro.path("numero").asText()).matches("CH-[0-9]{4}-[0-9]{6,}");
    assertThat(segundo.path("numero").asText()).isNotEqualTo(primeiro.path("numero").asText());
    assertThat(primeiro.path("prioridade").asText()).isEqualTo("MEDIA");
    mvc.perform(get("/api/v1/chamados/meus").session(maria).param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content.length()").value(1));
    mvc.perform(get("/api/v1/chamados/meus").session(maria).param("sort", "email,asc"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/chamados/meus").session(maria).param("size", "101"))
        .andExpect(status().isBadRequest());
  }

  private JsonNode criar(MockHttpSession sessao, String titulo, Long solicitanteId)
      throws Exception {
    MvcResult resultado =
        mvc.perform(
                post("/api/v1/chamados")
                    .session(sessao)
                    .header("X-CSRF-TOKEN", csrf(sessao))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo(titulo, solicitanteId)))
            .andExpect(status().isCreated())
            .andReturn();
    return mapper.readTree(resultado.getResponse().getContentAsString());
  }

  private String corpo(String titulo, Long solicitanteId) throws Exception {
    var dados =
        new HashMap<String, Object>(
            Map.of(
                "titulo", titulo,
                "descricao", "Preciso de ajuda para resolver este problema.",
                "categoriaId", categoriaId));
    if (solicitanteId != null) dados.put("solicitanteId", solicitanteId);
    return mapper.writeValueAsString(dados);
  }

  private MockHttpSession entrar(String email, String nome) throws Exception {
    MvcResult token = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
    MvcResult login =
        mvc.perform(
                post("/api/v1/auth/dev/login")
                    .session((MockHttpSession) token.getRequest().getSession(false))
                    .header("X-CSRF-TOKEN", valorToken(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Map.of("email", email, "nome", nome))))
            .andExpect(status().isOk())
            .andReturn();
    return (MockHttpSession) login.getRequest().getSession(false);
  }

  private String csrf(MockHttpSession sessao) throws Exception {
    return valorToken(
        mvc.perform(get("/api/v1/auth/csrf").session(sessao))
            .andExpect(status().isOk())
            .andReturn());
  }

  private String valorToken(MvcResult resultado) throws Exception {
    return mapper.readTree(resultado.getResponse().getContentAsString()).path("token").asText();
  }
}
