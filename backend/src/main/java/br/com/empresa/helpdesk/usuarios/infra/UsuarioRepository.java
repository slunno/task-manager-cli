package br.com.empresa.helpdesk.usuarios.infra;

import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  Optional<Usuario> findByEmailIgnoreCase(String email);
}
