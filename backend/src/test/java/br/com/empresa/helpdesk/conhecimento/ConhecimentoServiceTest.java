package br.com.empresa.helpdesk.conhecimento;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.empresa.helpdesk.admin.application.CategoriaService;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.conhecimento.application.ConhecimentoService;
import br.com.empresa.helpdesk.conhecimento.domain.Artigo;
import br.com.empresa.helpdesk.conhecimento.infra.ArtigoRepository;
import br.com.empresa.helpdesk.conhecimento.infra.FiltroSalvoRepository;
import br.com.empresa.helpdesk.conhecimento.infra.RespostaProntaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConhecimentoServiceTest {
  final ArtigoRepository artigos = mock(ArtigoRepository.class);
  final CategoriaService categorias = mock(CategoriaService.class);
  final Instant now = Instant.parse("2026-09-28T12:00:00Z");
  final ConhecimentoService service =
      new ConhecimentoService(
          artigos,
          mock(RespostaProntaRepository.class),
          mock(FiltroSalvoRepository.class),
          categorias,
          Clock.fixed(now, ZoneOffset.UTC));

  @Test
  void despublicarRemoveAcessoPublicoSemAlterarAutoriaECriacao() {
    var criado = now.minusSeconds(60);
    var artigo = new Artigo("Orientação", "Conteúdo", null, true, 7L, criado);
    when(artigos.findById(1L)).thenReturn(Optional.of(artigo));
    when(artigos.saveAndFlush(artigo)).thenReturn(artigo);
    assertThat(service.artigo(1L, false)).isSameAs(artigo);
    service.editarArtigo(1L, " Revisado ", " Conteúdo novo ", null, false);
    assertThatThrownBy(() -> service.artigo(1L, false))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    assertThat(service.artigo(1L, true)).isSameAs(artigo);
    assertThat(artigo.getAutorId()).isEqualTo(7L);
    assertThat(artigo.getCriadoEm()).isEqualTo(criado);
    assertThat(artigo.getAtualizadoEm()).isEqualTo(now);
  }

  @Test
  void categoriaInativaImpedeEdicaoSemMutacaoParcial() {
    var artigo = new Artigo("Original", "Conteúdo", null, true, 7L, now);
    when(artigos.findById(1L)).thenReturn(Optional.of(artigo));
    doThrow(new RecursoNaoEncontradoException("Categoria inativa"))
        .when(categorias)
        .exigirAtiva(99L);
    assertThatThrownBy(() -> service.editarArtigo(1L, "Outro", "Outro", 99L, false))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    assertThat(artigo.getTitulo()).isEqualTo("Original");
    assertThat(artigo.isPublicado()).isTrue();
    verify(artigos, never()).saveAndFlush(any());
  }
}
