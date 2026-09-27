package br.com.empresa.helpdesk.admin;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class AdministracaoIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired UsuarioRepository usuarios;

  @Test
  void somenteAdminPodeLerEAlterarConfiguracoes() throws Exception {
    MockHttpSession funcionario = entrar("e7-funcionario@empresa.com");
    for (String recurso : new String[] {"usuarios", "categorias", "slas", "calendario"}) {
      mvc.perform(get("/api/v1/ti/admin/" + recurso).session(funcionario))
          .andExpect(status().isForbidden());
    }
    String email = "e7-admin@empresa.com";
    Usuario admin =
        usuarios
            .findByEmailIgnoreCase(email)
            .orElseGet(() -> Usuario.novoFuncionario("Admin E7", email));
    admin.alterarPerfil(Perfil.TI_ADMIN);
    usuarios.saveAndFlush(admin);
    MockHttpSession sessao = entrar(email);
    mvc.perform(get("/api/v1/ti/admin/usuarios").session(sessao)).andExpect(status().isOk());
    mvc.perform(
            put("/api/v1/ti/admin/slas/ALTA")
                .session(sessao)
                .header("X-CSRF-TOKEN", csrf(sessao))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"horasPrimeiraResposta\":3,\"horasResolucao\":12}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.horasResolucao").value(12));
    mvc.perform(
            put("/api/v1/ti/admin/slas/ALTA")
                .session(sessao)
                .header("X-CSRF-TOKEN", csrf(sessao))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"horasPrimeiraResposta\":13,\"horasResolucao\":12}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/ti/admin/categorias")
                .session(sessao)
                .header("X-CSRF-TOKEN", csrf(sessao))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Categoria E7\"}"))
        .andExpect(status().isCreated());
    mvc.perform(
            post("/api/v1/ti/admin/calendario/feriados")
                .session(sessao)
                .header("X-CSRF-TOKEN", csrf(sessao))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"data\":\"2027-01-01\",\"descricao\":\"Ano Novo\"}"))
        .andExpect(status().isOk());
    mvc.perform(
            multipart("/api/v1/ti/admin/usuarios/importacao")
                .file(
                    new MockMultipartFile(
                        "arquivo",
                        "usuarios.csv",
                        "text/csv",
                        "nome;email;perfil;ativo\nImportado E7;importado-e7@empresa.com;TI_AGENTE;true\nInvalido;ruim;TI_AGENTE;true"
                            .getBytes()))
                .session(sessao)
                .header("X-CSRF-TOKEN", csrf(sessao)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.importados").value(1))
        .andExpect(jsonPath("$.rejeitados").value(1));
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
    var resultado = mvc.perform(get("/api/v1/auth/csrf").session(sessao)).andReturn();
    return mapper.readTree(resultado.getResponse().getContentAsString()).path("token").asText();
  }
}
