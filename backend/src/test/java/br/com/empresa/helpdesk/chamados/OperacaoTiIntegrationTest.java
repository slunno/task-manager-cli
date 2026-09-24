package br.com.empresa.helpdesk.chamados;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.empresa.helpdesk.admin.domain.Categoria;
import br.com.empresa.helpdesk.admin.infra.CategoriaRepository;
import br.com.empresa.helpdesk.chamados.domain.Chamado;
import br.com.empresa.helpdesk.chamados.domain.Prioridade;
import br.com.empresa.helpdesk.chamados.infra.ChamadoRepository;
import br.com.empresa.helpdesk.historico.infra.HistoricoChamadoRepository;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class OperacaoTiIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper mapper;
  @Autowired private ChamadoRepository chamados;
  @Autowired private HistoricoChamadoRepository historico;
  @Autowired private CategoriaRepository categorias;
  @Autowired private UsuarioRepository usuarios;
  @Autowired private JdbcTemplate jdbc;
  private Long categoriaId;

  @BeforeEach
  void preparar() {
    historico.deleteAll();
    chamados.deleteAll();
    categorias.deleteAll();
    usuarios.deleteAll();
    categoriaId = categorias.saveAndFlush(new Categoria("Equipamentos")).getId();
  }

  @Test
  void funcionarioNaoAcessaFilaMutacoesNemHistoricoDaTi() throws Exception {
    MockHttpSession maria = entrar("maria@empresa.com", "Maria");
    long id = criar(maria, "Acesso bloqueado").path("id").asLong();
    mvc.perform(get("/api/v1/chamados").session(maria)).andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/chamados/" + id + "/historico").session(maria))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/chamados/" + id + "/assumir")
                .session(maria)
                .header("X-CSRF-TOKEN", csrf(maria))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("version", 0))))
        .andExpect(status().isForbidden());
    mvc.perform(
            patch("/api/v1/chamados/" + id)
                .session(maria)
                .header("X-CSRF-TOKEN", csrf(maria))
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("version", 0, "prioridade", "ALTA"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void buscaPessoasEExclusivaDaTiEFiltraResponsaveisAtivos() throws Exception {
    MockHttpSession maria = entrar("maria@empresa.com", "Maria");
    MockHttpSession agente = entrar("agente@empresa.com", "Agente");
    promover("agente@empresa.com", Perfil.TI_AGENTE);
    mvc.perform(get("/api/v1/usuarios/busca").session(maria).param("texto", "ma"))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/usuarios/busca").session(agente).param("texto", "ma"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].nome").value("Maria"));
    mvc.perform(
            get("/api/v1/usuarios/busca")
                .session(agente)
                .param("texto", "ma")
                .param("somenteTi", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/v1/usuarios/busca").session(agente).param("texto", "m"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void filaAplicaFiltrosPaginacaoESetorSemVazarParaFuncionario() throws Exception {
    MockHttpSession maria = entrar("maria@empresa.com", "Maria");
    MockHttpSession joao = entrar("joao@empresa.com", "João");
    MockHttpSession agente = entrar("agente@empresa.com", "Agente");
    promover("agente@empresa.com", Perfil.TI_AGENTE);
    long idMaria = criar(maria, "VPN sem acesso").path("id").asLong();
    criar(joao, "Notebook parado");
    Long mariaId = usuarios.findByEmailIgnoreCase("maria@empresa.com").orElseThrow().getId();
    jdbc.update("update usuarios set setor_id = ? where id = ?", 7L, mariaId);

    mvc.perform(get("/api/v1/chamados").session(agente).param("texto", "vpn"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(idMaria));
    mvc.perform(get("/api/v1/chamados").session(agente).param("setorId", "7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(
            get("/api/v1/chamados")
                .session(agente)
                .param("semResponsavel", "true")
                .param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.content.length()").value(1));
    mvc.perform(get("/api/v1/chamados").session(agente).param("sort", "email,asc"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/chamados").session(agente).param("size", "101"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void assumirRecusaVersaoAntigaERegistraHistorico() throws Exception {
    MockHttpSession maria = entrar("maria@empresa.com", "Maria");
    MockHttpSession primeiro = entrar("primeiro@empresa.com", "Primeiro");
    MockHttpSession segundo = entrar("segundo@empresa.com", "Segundo");
    promover("primeiro@empresa.com", Perfil.TI_AGENTE);
    promover("segundo@empresa.com", Perfil.TI_AGENTE);
    long id = criar(maria, "Impressora parada").path("id").asLong();
    long primeiroId = usuarios.findByEmailIgnoreCase("primeiro@empresa.com").orElseThrow().getId();

    JsonNode assumido = assumir(primeiro, id, 0, 200);
    assertThat(assumido.path("status").asText()).isEqualTo("EM_ATENDIMENTO");
    assertThat(assumido.path("responsavelId").asLong()).isEqualTo(primeiroId);
    assumir(segundo, id, 0, 409);
    assumir(segundo, id, 1, 409);
    mvc.perform(get("/api/v1/chamados/" + id + "/historico").session(primeiro))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(2));
    mvc.perform(get("/api/v1/chamados").session(primeiro).param("meus", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
    mvc.perform(get("/api/v1/chamados").session(segundo).param("meus", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void administradorAtualizaPrioridadeCategoriaETransicoesComSolucao() throws Exception {
    MockHttpSession maria = entrar("maria@empresa.com", "Maria");
    MockHttpSession admin = entrar("admin@empresa.com", "Admin");
    promover("admin@empresa.com", Perfil.TI_ADMIN);
    long id = criar(maria, "Notebook parado").path("id").asLong();
    JsonNode assumido = assumir(admin, id, 0, 200);
    int versao = assumido.path("version").asInt();
    Long outraCategoria = categorias.saveAndFlush(new Categoria("Rede")).getId();

    JsonNode atualizado =
        alterar(
            admin,
            id,
            Map.of(
                "version",
                versao,
                "prioridade",
                "ALTA",
                "categoriaId",
                outraCategoria,
                "status",
                "AGUARDANDO_USUARIO"),
            200);
    assertThat(atualizado.path("prioridade").asText()).isEqualTo("ALTA");
    assertThat(atualizado.path("categoriaId").asLong()).isEqualTo(outraCategoria);
    alterar(admin, id, Map.of("version", versao, "status", "EM_ATENDIMENTO"), 409);
    versao = atualizado.path("version").asInt();
    atualizado = alterar(admin, id, Map.of("version", versao, "status", "EM_ATENDIMENTO"), 200);
    versao = atualizado.path("version").asInt();
    alterar(admin, id, Map.of("version", versao, "status", "FECHADO"), 409);
    alterar(admin, id, Map.of("version", versao, "status", "RESOLVIDO"), 400);
    atualizado =
        alterar(
            admin,
            id,
            Map.of("version", versao, "status", "RESOLVIDO", "solucao", "Equipamento substituído"),
            200);
    assertThat(atualizado.path("solucao").asText()).isEqualTo("Equipamento substituído");
    assertThat(atualizado.path("resolvidoEm").asText()).isNotBlank();
    mvc.perform(get("/api/v1/chamados/" + id + "/historico").session(admin))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(7))
        .andExpect(jsonPath("$.content[0].valorNovo").value("RESOLVIDO"));
  }

  @Test
  void versionJpaImpedeDuasGravacoesDoMesmoEstado() throws Exception {
    MockHttpSession maria = entrar("maria@empresa.com", "Maria");
    long id = criar(maria, "VPN indisponível").path("id").asLong();
    Chamado copiaA = chamados.findById(id).orElseThrow();
    Chamado copiaB = chamados.findById(id).orElseThrow();
    copiaA.alterarPrioridade(Prioridade.ALTA, Instant.now());
    copiaB.alterarPrioridade(Prioridade.BAIXA, Instant.now());
    chamados.saveAndFlush(copiaA);
    assertThatThrownBy(() -> chamados.saveAndFlush(copiaB))
        .isInstanceOf(OptimisticLockingFailureException.class);
  }

  private JsonNode assumir(MockHttpSession sessao, long id, int version, int esperado)
      throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/v1/chamados/" + id + "/assumir")
                    .session(sessao)
                    .header("X-CSRF-TOKEN", csrf(sessao))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Map.of("version", version))))
            .andExpect(status().is(esperado))
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode alterar(MockHttpSession sessao, long id, Map<String, Object> dados, int esperado)
      throws Exception {
    MvcResult result =
        mvc.perform(
                patch("/api/v1/chamados/" + id)
                    .session(sessao)
                    .header("X-CSRF-TOKEN", csrf(sessao))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(dados)))
            .andExpect(status().is(esperado))
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  private JsonNode criar(MockHttpSession sessao, String titulo) throws Exception {
    MvcResult result =
        mvc.perform(
                post("/api/v1/chamados")
                    .session(sessao)
                    .header("X-CSRF-TOKEN", csrf(sessao))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsString(
                            Map.of(
                                "titulo",
                                titulo,
                                "descricao",
                                "Preciso de ajuda para este problema.",
                                "categoriaId",
                                categoriaId))))
            .andExpect(status().isCreated())
            .andReturn();
    return mapper.readTree(result.getResponse().getContentAsString());
  }

  private void promover(String email, Perfil perfil) {
    var usuario = usuarios.findByEmailIgnoreCase(email).orElseThrow();
    usuario.alterarPerfil(perfil);
    usuarios.saveAndFlush(usuario);
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

  private String valorToken(MvcResult result) throws Exception {
    return mapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
  }
}
