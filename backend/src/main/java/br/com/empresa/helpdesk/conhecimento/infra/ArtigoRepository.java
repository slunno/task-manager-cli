package br.com.empresa.helpdesk.conhecimento.infra;

import br.com.empresa.helpdesk.conhecimento.domain.Artigo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtigoRepository extends JpaRepository<Artigo, Long> {
  @Query(
      "select a from Artigo a where (:incluirRascunhos = true or a.publicado = true) and (:categoriaId is null or a.categoriaId = :categoriaId) and (:texto = '' or lower(a.titulo) like concat('%', :texto, '%') or lower(a.conteudo) like concat('%', :texto, '%'))")
  Page<Artigo> buscar(
      @Param("texto") String texto,
      @Param("categoriaId") Long categoriaId,
      @Param("incluirRascunhos") boolean incluirRascunhos,
      Pageable pageable);
}
