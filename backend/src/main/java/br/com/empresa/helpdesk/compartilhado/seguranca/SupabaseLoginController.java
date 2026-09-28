package br.com.empresa.helpdesk.compartilhado.seguranca;

import br.com.empresa.helpdesk.usuarios.api.UsuarioMapper;
import br.com.empresa.helpdesk.usuarios.api.UsuarioResponse;
import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("supabase-auth")
public class SupabaseLoginController {
  private final SupabasePasswordClient auth;
  private final UsuarioService usuarios;
  private final UsuarioMapper mapper;
  private final SecurityContextRepository contextRepository;

  public SupabaseLoginController(
      SupabasePasswordClient auth,
      UsuarioService usuarios,
      UsuarioMapper mapper,
      SecurityContextRepository contextRepository) {
    this.auth = auth;
    this.usuarios = usuarios;
    this.mapper = mapper;
    this.contextRepository = contextRepository;
  }

  @PostMapping("/api/v1/auth/password/login")
  public ResponseEntity<UsuarioResponse> login(
      @Valid @RequestBody LoginRequest dados,
      HttpServletRequest request,
      HttpServletResponse response) {
    String email;
    try {
      email = auth.autenticar(dados.email().trim(), dados.senha());
    } catch (SupabasePasswordClient.LoginRecusadoException ex) {
      throw recusado();
    }
    var usuario = usuarios.buscarAtivo(email).orElseThrow(SupabaseLoginController::recusado);
    request.getSession(true);
    request.changeSessionId();
    var contexto = SecurityContextHolder.createEmptyContext();
    contexto.setAuthentication(
        new UsernamePasswordAuthenticationToken(
            new PrincipalDev(usuario.getId(), usuario.getEmail()),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name()))));
    SecurityContextHolder.setContext(contexto);
    contextRepository.saveContext(contexto, request, response);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(mapper.paraResponse(usuario));
  }

  private static ResponseStatusException recusado() {
    return new ResponseStatusException(
        HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos, ou acesso indisponível.");
  }

  public record LoginRequest(
      @NotBlank @Email @Size(max = 180) String email, @NotBlank @Size(max = 256) String senha) {
    @Override
    public String toString() {
      return "LoginRequest[credenciais omitidas]";
    }
  }
}
