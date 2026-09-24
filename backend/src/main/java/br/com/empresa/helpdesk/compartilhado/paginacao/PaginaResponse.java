package br.com.empresa.helpdesk.compartilhado.paginacao;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResponse<T>(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<T> content,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int page,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int size,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long totalElements,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int totalPages) {
  public static <T> PaginaResponse<T> de(Page<T> pagina) {
    return new PaginaResponse<>(
        pagina.getContent(),
        pagina.getNumber(),
        pagina.getSize(),
        pagina.getTotalElements(),
        pagina.getTotalPages());
  }
}
