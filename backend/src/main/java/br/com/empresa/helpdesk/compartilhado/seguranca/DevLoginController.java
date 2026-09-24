package br.com.empresa.helpdesk.compartilhado.seguranca;

import br.com.empresa.helpdesk.usuarios.api.UsuarioMapper;
import br.com.empresa.helpdesk.usuarios.api.UsuarioResponse;
import br.com.empresa.helpdesk.usuarios.application.UsuarioService;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev & !prod")
public class DevLoginController {
  private final UsuarioService usuarios;
  private final UsuarioMapper mapper;
  private final SecurityContextRepository contextRepository;

  public DevLoginController(
      UsuarioService usuarios, UsuarioMapper mapper, SecurityContextRepository contextRepository) {
    this.usuarios = usuarios;
    this.mapper = mapper;
    this.contextRepository = contextRepository;
  }

  @PostMapping("/api/v1/auth/dev/login")
  public ResponseEntity<UsuarioResponse> login(
      @Valid @RequestBody LoginRequest dados,
      HttpServletRequest request,
      HttpServletResponse response) {
    Usuario usuario = usuarios.provisionarNoLogin(dados.email(), dados.nome());
    request.getSession(true);
    request.changeSessionId();
    var principal = new PrincipalDev(usuario.getId(), usuario.getEmail());
    var autenticacao =
        new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
    var contexto = SecurityContextHolder.createEmptyContext();
    contexto.setAuthentication(autenticacao);
    SecurityContextHolder.setContext(contexto);
    contextRepository.saveContext(contexto, request, response);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .body(mapper.paraResponse(usuario));
  }

  public record LoginRequest(
      @NotBlank @Email String email, @NotBlank @Size(max = 180) String nome) {}
}
