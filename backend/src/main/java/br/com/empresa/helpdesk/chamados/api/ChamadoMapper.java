package br.com.empresa.helpdesk.chamados.api;

import br.com.empresa.helpdesk.chamados.domain.Chamado;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChamadoMapper {
  ChamadoResponse paraResponse(Chamado chamado);
}
