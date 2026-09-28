package br.com.empresa.helpdesk.usuarios;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.empresa.helpdesk.compartilhado.seguranca.SupabasePasswordClient;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "helpdesk.auth.supabase.url=https://auth.example",
      "helpdesk.auth.supabase.publishable-key=sb_publishable_test"
    })
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test", "supabase-auth"})
@Transactional
class SupabaseIdentidadeIntegrationTest {
  @Autowired MockMvc mvc;
  @Autowired UsuarioRepository usuarios;
  @MockitoBean SupabasePasswordClient auth;

  @Test
  void loginRealCriaSessaoComPerfilDoBancoEUsuarioDesativadoPerdeAcesso() throws Exception {
    var usuario = Usuario.novoFuncionario("TI teste", "supabase-ti@example.com");
    usuario.alterarPerfil(Perfil.TI_ADMIN);
    usuarios.saveAndFlush(usuario);
    when(auth.autenticar("supabase-ti@example.com", "senha")).thenReturn("supabase-ti@example.com");
    var login =
        mvc.perform(
                post("/api/v1/auth/password/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"supabase-ti@example.com\",\"senha\":\"senha\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perfil").value("TI_ADMIN"))
            .andReturn();
    var sessao = (MockHttpSession) login.getRequest().getSession(false);
    mvc.perform(get("/api/v1/me").session(sessao)).andExpect(status().isOk());
    usuario.desativar();
    usuarios.saveAndFlush(usuario);
    mvc.perform(get("/api/v1/me").session(sessao)).andExpect(status().isUnauthorized());
  }

  @Test
  void csrfObrigatorioELoginSimuladoDesativado() throws Exception {
    mvc.perform(get("/api/v1/auth/config"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.modo").value("supabase"));
    mvc.perform(
            post("/api/v1/auth/password/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"supabase-ti@example.com\",\"senha\":\"senha\"}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/auth/dev/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"supabase-ti@example.com\",\"nome\":\"TI\"}"))
        .andExpect(status().isNotFound());
  }
}
