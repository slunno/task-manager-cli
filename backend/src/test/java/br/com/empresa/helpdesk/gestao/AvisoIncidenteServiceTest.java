package br.com.empresa.helpdesk.gestao;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.empresa.helpdesk.compartilhado.erros.RecursoNaoEncontradoException;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.gestao.application.AvisoIncidenteService;
import br.com.empresa.helpdesk.gestao.domain.AvisoIncidente;
import br.com.empresa.helpdesk.gestao.infra.AvisoIncidenteRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AvisoIncidenteServiceTest {
  final AvisoIncidenteRepository repository = mock(AvisoIncidenteRepository.class);
  final Instant now = Instant.parse("2026-09-28T12:00:00Z");
  final AvisoIncidenteService service =
      new AvisoIncidenteService(repository, Clock.fixed(now, ZoneOffset.UTC));

  @Test
  void rejeitaInicioAusenteEFimAnteriorOuIgual() {
    assertThatThrownBy(() -> service.criar("Aviso", "Teste", null, null, 1L))
        .isInstanceOf(RequisicaoInvalidaException.class);
    for (var end : new Instant[] {now, now.minusSeconds(1)}) {
      assertThatThrownBy(() -> service.criar("Aviso", "Teste", now, end, 1L))
          .isInstanceOf(RequisicaoInvalidaException.class);
    }
    verify(repository, never()).saveAndFlush(any());
  }

  @Test
  void permiteAvisoSemFimEDesativacaoPosterior() {
    var aviso = new AvisoIncidente("Aviso", "Teste", now, null, 1L, now);
    when(repository.findById(1L)).thenReturn(Optional.of(aviso));
    when(repository.saveAndFlush(aviso)).thenReturn(aviso);
    var result = service.editar(1L, " Atualizado ", " Mensagem ", now, now.plusSeconds(60), false);
    assertThat(result.isAtivo()).isFalse();
    assertThat(result.getFimEm()).isEqualTo(now.plusSeconds(60));
    assertThat(result.getTitulo()).isEqualTo("Atualizado");
  }

  @Test
  void naoEditaAvisoAusenteNemAlteraAntesDeValidarPeriodo() {
    assertThatThrownBy(() -> service.editar(99L, "Aviso", "Teste", now, null, true))
        .isInstanceOf(RecursoNaoEncontradoException.class);
    assertThatThrownBy(() -> service.editar(1L, "Aviso", "Teste", now, now, true))
        .isInstanceOf(RequisicaoInvalidaException.class);
    verify(repository, never()).findById(1L);
  }
}
