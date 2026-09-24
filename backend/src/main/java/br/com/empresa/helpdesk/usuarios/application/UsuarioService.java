package br.com.empresa.helpdesk.usuarios.application;

import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import br.com.empresa.helpdesk.usuarios.infra.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

  @Transactional(readOnly = true)
  public List<Usuario> buscarAtivos(String texto, boolean somenteTi) {
    if (texto == null || texto.trim().length() < 2 || texto.trim().length() > 100) {
      throw new RequisicaoInvalidaException("Informe entre 2 e 100 caracteres para buscar pessoas");
    }
    String termo =
        texto
            .trim()
            .toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    return repository
        .buscarAtivos(
            "%" + termo + "%",
            somenteTi,
            PageRequest.of(0, 20, Sort.by("nome").ascending().and(Sort.by("id"))))
        .getContent();
  }

  private String normalizarEmail(String email) {
    if (email == null || email.isBlank() || !email.contains("@")) {
      throw new IllegalArgumentException("Identidade sem e-mail válido");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
