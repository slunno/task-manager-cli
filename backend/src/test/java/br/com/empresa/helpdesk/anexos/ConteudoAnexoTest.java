package br.com.empresa.helpdesk.anexos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.empresa.helpdesk.anexos.application.*;
import br.com.empresa.helpdesk.anexos.infra.AnexoRepository;
import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.chamados.domain.Chamado;
import br.com.empresa.helpdesk.chamados.domain.StatusChamado;
import br.com.empresa.helpdesk.comentarios.application.ComentarioService;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.time.Clock;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockMultipartFile;

class ConteudoAnexoTest {
  private final ConteudoAnexo conteudo =
      new ConteudoAnexo("pdf,png,jpg,jpeg,txt,gif,webp,csv,log,docx,xlsx,zip");

  @ParameterizedTest
  @MethodSource("br.com.empresa.helpdesk.anexos.ArquivosAnexoTeste#amostras")
  void validaAssinaturaDeCadaNovoTipo(String ext, String mime, byte[] dados) {
    assertThat(conteudo.validar(dados, mime, "teste." + ext)).isEqualTo(mime);
    assertThatThrownBy(() -> conteudo.validar(new byte[] {77, 90, 0, 0}, mime, "falso." + ext))
        .isInstanceOf(RequisicaoInvalidaException.class);
  }

  @Test
  void rejeitaZipBombCaminhoPerigosoScriptsEntradasDemaisETruncamento() {
    for (Map<String, byte[]> entradas :
        java.util.List.of(
            Map.of("muito.txt", new byte[2 * 1024 * 1024]), Map.of("../fora.txt", new byte[] {1}),
            Map.of("script.js", new byte[] {1}), Map.of("programa.exe", new byte[] {1}))) {
      assertThatThrownBy(
              () ->
                  conteudo.validar(
                      ArquivosAnexoTeste.zip(entradas), "application/zip", "teste.zip"))
          .isInstanceOf(RequisicaoInvalidaException.class);
    }
    var entradas = new java.util.HashMap<String, byte[]>();
    for (int i = 0; i < 1001; i++) entradas.put("arquivo" + i + ".txt", new byte[] {1});
    assertThatThrownBy(
            () ->
                conteudo.validar(ArquivosAnexoTeste.zip(entradas), "application/zip", "teste.zip"))
        .isInstanceOf(RequisicaoInvalidaException.class);
    byte[] zip = ArquivosAnexoTeste.zip(Map.of("teste.txt", new byte[] {1}));
    assertThatThrownBy(
            () ->
                conteudo.validar(
                    java.util.Arrays.copyOf(zip, zip.length - 22), "application/zip", "teste.zip"))
        .isInstanceOf(RequisicaoInvalidaException.class);
  }

  @Test
  void docxExigeEstruturaCorretaEListaConfiguravelNaoPermiteExecutaveis() {
    assertThatThrownBy(
            () ->
                conteudo.validar(
                    ArquivosAnexoTeste.zip(Map.of("arquivo.txt", new byte[] {1})),
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "teste.docx"))
        .isInstanceOf(RequisicaoInvalidaException.class);
    assertThatThrownBy(() -> new ConteudoAnexo("exe,pdf"))
        .isInstanceOf(IllegalArgumentException.class);
    var restrito = new ConteudoAnexo("pdf");
    assertThatThrownBy(() -> restrito.validar("Texto".getBytes(), "text/plain", "teste.txt"))
        .isInstanceOf(RequisicaoInvalidaException.class);
  }

  @Test
  void scannerPodeRejeitarAntesDeGravar() {
    var storage = mock(StorageService.class);
    var chamados = mock(ChamadoService.class);
    var chamado = mock(Chamado.class);
    when(chamado.getStatus()).thenReturn(StatusChamado.ABERTO);
    var usuario = Usuario.novoFuncionario("Fictício", "ficticio@example.com");
    when(chamados.exigirAcesso(1L, usuario)).thenReturn(chamado);
    ScannerAnexo scanner =
        (dados, mime) -> {
          throw new RequisicaoInvalidaException("Anexo rejeitado pelo scanner");
        };
    var service =
        new AnexoService(
            mock(AnexoRepository.class),
            mock(ComentarioService.class),
            chamados,
            storage,
            Clock.systemUTC(),
            conteudo,
            scanner);
    assertThatThrownBy(
            () ->
                service.enviar(
                    1L,
                    null,
                    false,
                    new MockMultipartFile(
                        "arquivo", "teste.txt", "text/plain", "Diagnóstico".getBytes()),
                    usuario))
        .isInstanceOf(RequisicaoInvalidaException.class);
    verifyNoInteractions(storage);
  }
}
