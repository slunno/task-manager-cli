package br.com.empresa.helpdesk.usuarios.api;

import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {
  UsuarioResponse paraResponse(Usuario usuario);
}
