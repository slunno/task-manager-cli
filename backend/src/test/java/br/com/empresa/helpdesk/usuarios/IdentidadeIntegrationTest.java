package br.com.empresa.helpdesk.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class IdentidadeIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private UsuarioRepository usuarios;
  @Autowired private ObjectMapper mapper;

  @BeforeEach
  void limpar() {
    usuarios.deleteAll();
  }

  @Test
  void primeiroLoginProvisionaFuncionarioEReusaUsuario() throws Exception {
    MockHttpSession sessao = entrar("pessoa@empresa.com", "Pessoa");
    mvc.perform(get("/api/v1/me").session(sessao))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.perfil").value("FUNCIONARIO"))
        .andExpect(jsonPath("$.email").value("pessoa@empresa.com"));
    assertThat(usuarios.count()).isEqualTo(1);
    entrar("PESSOA@empresa.com", "Pessoa");
    assertThat(usuarios.count()).isEqualTo(1);
  }

  @Test
  void perfilAtualizaNaSessaoEInativoPerdeAcesso() throws Exception {
    MockHttpSession sessao = entrar("ti@empresa.com", "Agente");
    var usuario = usuarios.findByEmailIgnoreCase("ti@empresa.com").orElseThrow();
    usuario.alterarPerfil(Perfil.TI_AGENTE);
    usuarios.saveAndFlush(usuario);
    mvc.perform(get("/api/v1/me").session(sessao))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.perfil").value("TI_AGENTE"));
    usuario.alterarPerfil(Perfil.TI_ADMIN);
    usuarios.saveAndFlush(usuario);
    mvc.perform(get("/api/v1/me").session(sessao))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.perfil").value("TI_ADMIN"));
    usuario.desativar();
    usuarios.saveAndFlush(usuario);
    mvc.perform(get("/api/v1/me").session(sessao)).andExpect(status().isUnauthorized());
  }

  @Test
  void loginInativoEhBarrado() throws Exception {
    entrar("inativo@empresa.com", "Pessoa");
    var usuario = usuarios.findByEmailIgnoreCase("inativo@empresa.com").orElseThrow();
    usuario.desativar();
    usuarios.saveAndFlush(usuario);
    MvcResult csrf = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
    mvc.perform(
            post("/api/v1/auth/dev/login")
                .session((MockHttpSession) csrf.getRequest().getSession(false))
                .header("X-CSRF-TOKEN", token(csrf))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"inativo@empresa.com\",\"nome\":\"Pessoa\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void csrfProtegeLoginELogout() throws Exception {
    mvc.perform(
            post("/api/v1/auth/dev/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"x@empresa.com\",\"nome\":\"X\"}"))
        .andExpect(status().isForbidden());
    MockHttpSession sessao = entrar("x@empresa.com", "X");
    mvc.perform(post("/api/v1/auth/logout").session(sessao)).andExpect(status().isForbidden());
    MvcResult csrf =
        mvc.perform(get("/api/v1/auth/csrf").session(sessao))
            .andExpect(status().isOk())
            .andReturn();
    mvc.perform(post("/api/v1/auth/logout").session(sessao).header("X-CSRF-TOKEN", token(csrf)))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/me").session(sessao)).andExpect(status().isUnauthorized());
  }

  private MockHttpSession entrar(String email, String nome) throws Exception {
    MvcResult csrf = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
    String corpo = mapper.writeValueAsString(new Login(email, nome));
    MvcResult login =
        mvc.perform(
                post("/api/v1/auth/dev/login")
                    .session((MockHttpSession) csrf.getRequest().getSession(false))
                    .header("X-CSRF-TOKEN", token(csrf))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
            .andExpect(status().isOk())
            .andReturn();
    return (MockHttpSession) login.getRequest().getSession(false);
  }

  private String token(MvcResult resultado) throws Exception {
    return mapper.readTree(resultado.getResponse().getContentAsString()).path("token").asText();
  }

  private record Login(String email, String nome) {}
}
