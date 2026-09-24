package br.com.empresa.helpdesk.chamados.infra;

import br.com.empresa.helpdesk.chamados.application.FiltroFila;
import br.com.empresa.helpdesk.chamados.domain.Chamado;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class FilaSpecifications {
  private FilaSpecifications() {}

  public static Specification<Chamado> filtrar(
      FiltroFila filtro, List<Long> solicitantesSetor, Long agenteId, Instant agora) {
    return (root, query, cb) -> {
      List<Predicate> partes = new ArrayList<>();
      if (filtro.status() != null) partes.add(cb.equal(root.get("status"), filtro.status()));
      if (filtro.prioridade() != null)
        partes.add(cb.equal(root.get("prioridade"), filtro.prioridade()));
      if (filtro.responsavelId() != null)
        partes.add(cb.equal(root.get("responsavelId"), filtro.responsavelId()));
      if (filtro.categoriaId() != null)
        partes.add(cb.equal(root.get("categoriaId"), filtro.categoriaId()));
      if (filtro.setorId() != null)
        partes.add(
            solicitantesSetor.isEmpty()
                ? cb.disjunction()
                : root.get("solicitanteId").in(solicitantesSetor));
      if (filtro.desde() != null)
        partes.add(cb.greaterThanOrEqualTo(root.get("criadoEm"), filtro.desde()));
      if (filtro.ate() != null) partes.add(cb.lessThan(root.get("criadoEm"), filtro.ate()));
      if (filtro.semResponsavel()) partes.add(cb.isNull(root.get("responsavelId")));
      if (filtro.meus()) partes.add(cb.equal(root.get("responsavelId"), agenteId));
      if (filtro.slaVencendo()) {
        partes.add(cb.greaterThan(root.get("prazoResolucao"), agora));
        partes.add(cb.lessThanOrEqualTo(root.get("prazoResolucao"), agora.plusSeconds(86400)));
      }
      if (filtro.texto() != null && !filtro.texto().isBlank()) {
        String texto =
            filtro
                .texto()
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        String padrao = "%" + texto + "%";
        partes.add(
            cb.or(
                cb.like(cb.lower(root.get("titulo")), padrao, '\\'),
                cb.like(cb.lower(root.get("numero")), padrao, '\\'),
                cb.like(cb.lower(root.get("descricao")), padrao, '\\')));
      }
      return cb.and(partes.toArray(new Predicate[0]));
    };
  }
}
