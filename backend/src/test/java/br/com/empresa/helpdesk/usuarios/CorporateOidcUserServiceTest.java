package br.com.empresa.helpdesk.usuarios;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.empresa.helpdesk.usuarios.application.CorporateOidcUserService;
import br.com.empresa.helpdesk.usuarios.application.UsuarioInativoException;
import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import br.com.empresa.helpdesk.usuarios.domain.Perfil;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.time.Instant;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

class CorporateOidcUserServiceTest {
  final UsuarioService usuarios = mock(UsuarioService.class);
  final CorporateOidcUserService service =
      new CorporateOidcUserService(usuarios, "empresa.example");

  @Test
  void rejeitaDominioExternoEmailAusenteEEmailNaoVerificado() {
    assertThatThrownBy(() -> service.loadUser(request("pessoa@externo.example", true)))
        .isInstanceOf(OAuth2AuthenticationException.class);
    assertThatThrownBy(() -> service.loadUser(request(null, true)))
        .isInstanceOf(OAuth2AuthenticationException.class);
    assertThatThrownBy(() -> service.loadUser(request("pessoa@empresa.example", false)))
        .isInstanceOf(OAuth2AuthenticationException.class);
    verifyNoInteractions(usuarios);
  }

  @Test
  void claimDePerfilNaoElevaPermissaoDoBanco() {
    var usuario = mock(Usuario.class);
    when(usuario.getPerfil()).thenReturn(Perfil.FUNCIONARIO);
    when(usuarios.provisionarNoLogin("pessoa@empresa.example", "Pessoa fictícia"))
        .thenReturn(usuario);
    var identity = service.loadUser(request("pessoa@empresa.example", true));
    assertThat(identity.getAuthorities())
        .extracting("authority")
        .containsExactly("ROLE_FUNCIONARIO");
  }

  @Test
  void identidadeCorporativaValidaNaoReativaUsuarioInativo() {
    when(usuarios.provisionarNoLogin("pessoa@empresa.example", "Pessoa fictícia"))
        .thenThrow(new UsuarioInativoException());
    assertThatThrownBy(() -> service.loadUser(request("pessoa@empresa.example", true)))
        .isInstanceOf(OAuth2AuthenticationException.class)
        .hasMessage("Usuário inativo");
  }

  private OidcUserRequest request(String email, boolean verified) {
    var registration =
        ClientRegistration.withRegistrationId("teste")
            .clientId("client-test")
            .clientSecret("fixture")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://portal.example/callback")
            .scope("openid", "email", "profile")
            .authorizationUri("https://idp.example/auth")
            .tokenUri("https://idp.example/token")
            .jwkSetUri("https://idp.example/jwks")
            .build();
    var claims = new HashMap<String, Object>();
    claims.put("sub", "identidade-ficticia");
    claims.put("name", "Pessoa fictícia");
    claims.put("email_verified", verified);
    claims.put("perfil", "TI_ADMIN");
    if (email != null) claims.put("email", email);
    var now = Instant.parse("2026-09-28T12:00:00Z");
    return new OidcUserRequest(
        registration,
        new OAuth2AccessToken(
            OAuth2AccessToken.TokenType.BEARER, "fixture", now, now.plusSeconds(3600)),
        new OidcIdToken("fixture", now, now.plusSeconds(3600), claims));
  }
}
