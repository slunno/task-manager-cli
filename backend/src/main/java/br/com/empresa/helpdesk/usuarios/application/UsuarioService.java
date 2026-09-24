package br.com.empresa.helpdesk.usuarios.application;

import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
  private final UsuarioRepository repository;
  private final Clock clock;

  public UsuarioService(UsuarioRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public Usuario provisionarNoLogin(String email, String nome) {
    String emailNormalizado = normalizarEmail(email);
    String nomeSeguro = nome == null || nome.isBlank() ? emailNormalizado : nome.trim();
    Usuario usuario =
        repository
            .findByEmailIgnoreCase(emailNormalizado)
            .orElseGet(
                () ->
                    repository.saveAndFlush(Usuario.novoFuncionario(nomeSeguro, emailNormalizado)));
    if (!usuario.isAtivo()) {
      throw new UsuarioInativoException();
    }
    usuario.registrarLogin(Instant.now(clock));
    return repository.save(usuario);
  }

  @Transactional(readOnly = true)
  public Optional<Usuario> buscarAtivo(String email) {
    return repository.findByEmailIgnoreCase(normalizarEmail(email)).filter(Usuario::isAtivo);
  }

  @Transactional(readOnly = true)
  public Optional<Usuario> buscarAtivoPorId(Long id) {
    return repository.findById(id).filter(Usuario::isAtivo);
  }

  @Transactional(readOnly = true)
  public List<Long> idsPorSetor(Long setorId) {
    return repository.idsPorSetor(setorId);
  }

  private String normalizarEmail(String email) {
    if (email == null || email.isBlank() || !email.contains("@")) {
      throw new IllegalArgumentException("Identidade sem e-mail válido");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
