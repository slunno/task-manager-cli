package br.com.empresa.helpdesk.compartilhado.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.empresa.helpdesk.usuarios.api.UsuarioMapper;
import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.server.ResponseStatusException;

class SupabaseLoginControllerTest {
  @AfterEach
  void limpar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void usaPerfilDoBancoERenovaSessaoSemGuardarSenha() {
    var auth = mock(SupabasePasswordClient.class);
    var usuarios = mock(UsuarioService.class);
    var usuario = mock(Usuario.class);
    when(auth.autenticar("ti@example.com", "senha")).thenReturn("ti@example.com");
    when(usuarios.buscarAtivo("ti@example.com")).thenReturn(Optional.of(usuario));
    when(usuario.getId()).thenReturn(2L);
    when(usuario.getEmail()).thenReturn("ti@example.com");
    when(usuario.getPerfil()).thenReturn(Perfil.TI_ADMIN);
    var controller =
        new SupabaseLoginController(
            auth, usuarios, mock(UsuarioMapper.class), new HttpSessionSecurityContextRepository());
    var request = new MockHttpServletRequest();
    var before = request.getSession().getId();
    controller.login(
        new SupabaseLoginController.LoginRequest("ti@example.com", "senha"),
        request,
        new MockHttpServletResponse());
    assertThat(request.getSession().getId()).isNotEqualTo(before);
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_TI_ADMIN");
    assertThat(authentication.getCredentials()).isNull();
  }

  @Test
  void identidadeValidaSemCadastroAtivoNaoEntra() {
    var auth = mock(SupabasePasswordClient.class);
    var usuarios = mock(UsuarioService.class);
    when(auth.autenticar("fora@example.com", "senha")).thenReturn("fora@example.com");
    when(usuarios.buscarAtivo("fora@example.com")).thenReturn(Optional.empty());
    var controller =
        new SupabaseLoginController(
            auth, usuarios, mock(UsuarioMapper.class), new HttpSessionSecurityContextRepository());
    assertThatThrownBy(
            () ->
                controller.login(
                    new SupabaseLoginController.LoginRequest("fora@example.com", "senha"),
                    new MockHttpServletRequest(),
                    new MockHttpServletResponse()))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    verify(usuarios, never()).provisionarNoLogin(anyString(), anyString());
  }
}
