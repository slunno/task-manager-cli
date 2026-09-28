package br.com.empresa.helpdesk.admin;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.empresa.helpdesk.admin.application.SetorService;
import br.com.empresa.helpdesk.admin.domain.Setor;
import br.com.empresa.helpdesk.admin.infra.SetorRepository;
import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SetorServiceTest {
  final SetorRepository repository = mock(SetorRepository.class);
  final Instant now = Instant.parse("2026-09-28T12:00:00Z");
  final SetorService service = new SetorService(repository, Clock.fixed(now, ZoneOffset.UTC));

  @Test
  void renomearParaNomeExistenteNaoAlteraSetor() {
    var setor = new Setor("TI", now);
    when(repository.findById(1L)).thenReturn(Optional.of(setor));
    when(repository.existsByNomeIgnoreCase("Financeiro")).thenReturn(true);
    assertThatThrownBy(() -> service.editar(1L, " Financeiro ", false))
        .isInstanceOf(RequisicaoInvalidaException.class);
    assertThat(setor.getNome()).isEqualTo("TI");
    assertThat(setor.isAtivo()).isTrue();
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void desativarComMesmoNomeIgnoraConflitoComProprioRegistro() {
    var setor = new Setor("TI", now);
    when(repository.findById(1L)).thenReturn(Optional.of(setor));
    when(repository.saveAndFlush(setor)).thenReturn(setor);
    assertThat(service.editar(1L, " ti ", false).isAtivo()).isFalse();
    verify(repository, never()).existsByNomeIgnoreCase(anyString());
  }

  @Test
  void setorAusenteOuInativoNaoPodeSerVinculado() {
    assertThatThrownBy(() -> service.exigirAtivo(99L))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    when(repository.existsByIdAndAtivoTrue(1L)).thenReturn(true);
    assertThatCode(() -> service.exigirAtivo(1L)).doesNotThrowAnyException();
    assertThatThrownBy(() -> service.editar(99L, "TI", true))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void criarDuplicadoNaoGravaRegistro() {
    when(repository.existsByNomeIgnoreCase("TI")).thenReturn(true);
    assertThatThrownBy(() -> service.criar(" TI ")).isInstanceOf(RequisicaoInvalidaException.class);
    verify(repository, never()).saveAndFlush(any());
  }
}
