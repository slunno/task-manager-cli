package br.com.empresa.helpdesk.usuarios.application;

import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
@Profile("prod")
public class CorporateOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
  private final OidcUserService delegate = new OidcUserService();
  private final UsuarioService usuarios;
  private final String dominioPermitido;

  public CorporateOidcUserService(
      UsuarioService usuarios,
      @Value("${helpdesk.auth.allowed-email-domain}") String dominioPermitido) {
    this.usuarios = usuarios;
    this.dominioPermitido = dominioPermitido.toLowerCase(Locale.ROOT);
  }

  @Override
  public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
    OidcUser identidade = delegate.loadUser(request);
    String email =
        identidade.getEmail() != null ? identidade.getEmail() : identidade.getPreferredUsername();
    if (email == null
        || !email.toLowerCase(Locale.ROOT).endsWith("@" + dominioPermitido)
        || Boolean.FALSE.equals(identidade.getClaim("email_verified"))) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("identidade_rejeitada"),
          "A identidade não pertence ao domínio corporativo configurado");
    }
    Usuario usuario;
    try {
      usuario = usuarios.provisionarNoLogin(email, identidade.getFullName());
    } catch (UsuarioInativoException ex) {
      throw new OAuth2AuthenticationException(
          new OAuth2Error("usuario_inativo"), "Usuário inativo", ex);
    }
    var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
    if (identidade.getUserInfo() == null) {
      return new DefaultOidcUser(autoridades, identidade.getIdToken(), "sub");
    }
    return new DefaultOidcUser(
        autoridades, identidade.getIdToken(), identidade.getUserInfo(), "sub");
  }
}
