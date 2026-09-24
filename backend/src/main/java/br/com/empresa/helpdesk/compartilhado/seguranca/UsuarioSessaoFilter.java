package br.com.empresa.helpdesk.compartilhado.seguranca;

import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class UsuarioSessaoFilter extends OncePerRequestFilter {
  public static final String ATRIBUTO_USUARIO = UsuarioSessaoFilter.class.getName() + ".usuario";

  private final UsuarioService usuarios;
  private final ObjectMapper mapper;

  public UsuarioSessaoFilter(UsuarioService usuarios, ObjectMapper mapper) {
    this.usuarios = usuarios;
    this.mapper = mapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacao == null || autenticacao instanceof AnonymousAuthenticationToken) {
      chain.doFilter(request, response);
      return;
    }
    String email = emailDaAutenticacao(autenticacao);
    Usuario usuario = email == null ? null : usuarios.buscarAtivo(email).orElse(null);
    if (usuario == null) {
      SecurityContextHolder.clearContext();
      if (request.getSession(false) != null) {
        request.getSession(false).invalidate();
      }
      ProblemaSeguranca.escrever(
          response, HttpStatus.UNAUTHORIZED, "Sessão inválida ou usuário inativo", mapper);
      return;
    }
    request.setAttribute(ATRIBUTO_USUARIO, usuario);
    var papeis = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()));
    if (!autenticacao.getAuthorities().equals(papeis)) {
      Authentication atualizada;
      if (autenticacao instanceof OAuth2AuthenticationToken oauth) {
        atualizada =
            new OAuth2AuthenticationToken(
                oauth.getPrincipal(), papeis, oauth.getAuthorizedClientRegistrationId());
      } else {
        atualizada =
            new UsernamePasswordAuthenticationToken(autenticacao.getPrincipal(), null, papeis);
      }
      SecurityContextHolder.getContext().setAuthentication(atualizada);
    }
    chain.doFilter(request, response);
  }

  private String emailDaAutenticacao(Authentication autenticacao) {
    if (autenticacao.getPrincipal() instanceof PrincipalDev principal) {
      return principal.email();
    }
    if (autenticacao.getPrincipal() instanceof OidcUser oidc) {
      return oidc.getEmail() != null ? oidc.getEmail() : oidc.getPreferredUsername();
    }
    return null;
  }
}
