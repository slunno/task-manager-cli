package br.com.empresa.helpdesk.usuarios.infra;

import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
  Optional<Usuario> findByEmailIgnoreCase(String email);

  @Query("select u.id from Usuario u where u.setorId = :setorId")
  List<Long> idsPorSetor(Long setorId);

  @Query(
      "select u from Usuario u where u.ativo = true and (:somenteTi = false or u.perfil <> br.com.empresa.helpdesk.usuarios.domain.Perfil.FUNCIONARIO) and (lower(u.nome) like :padrao escape '\\' or lower(u.email) like :padrao escape '\\')")
  Page<Usuario> buscarAtivos(String padrao, boolean somenteTi, Pageable pageable);
}
