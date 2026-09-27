package br.com.empresa.helpdesk.anexos.application;

import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class ConteudoAnexo {
  static final long LIMITE = 10L * 1024 * 1024;
  private static final Map<String, String> TIPOS =
      Map.of(
          "application/pdf", "pdf", "image/png", "png", "image/jpeg", "jpg", "text/plain", "txt");

  private ConteudoAnexo() {}

  static String validar(byte[] dados, String declarado, String nome) {
    if (dados.length == 0 || dados.length > LIMITE) {
      throw new RequisicaoInvalidaException("Anexo deve ter entre 1 byte e 10 MB");
    }
    String mime = declarado == null ? "" : declarado.toLowerCase().split(";", 2)[0].trim();
    String extensao = nome.toLowerCase(java.util.Locale.ROOT);
    boolean extensaoValida =
        extensao.endsWith("." + TIPOS.get(mime))
            || ("image/jpeg".equals(mime) && extensao.endsWith(".jpeg"));
    if (!TIPOS.containsKey(mime) || !extensaoValida || !compativel(dados, mime)) {
      throw new RequisicaoInvalidaException("Formato de anexo não permitido ou conteúdo inválido");
    }
    return mime;
  }

  private static boolean compativel(byte[] dados, String mime) {
    return switch (mime) {
      case "application/pdf" ->
          comeca(dados, new byte[] {'%', 'P', 'D', 'F', '-'}) && terminaPdf(dados);
      case "image/png" -> comeca(dados, new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
      case "image/jpeg" ->
          comeca(dados, new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff})
              && dados.length > 4
              && dados[dados.length - 2] == (byte) 0xff
              && dados[dados.length - 1] == (byte) 0xd9;
      case "text/plain" -> textoValido(dados);
      default -> false;
    };
  }

  private static boolean comeca(byte[] dados, byte[] prefixo) {
    if (dados.length < prefixo.length) return false;
    for (int i = 0; i < prefixo.length; i++) if (dados[i] != prefixo[i]) return false;
    return true;
  }

  private static boolean terminaPdf(byte[] dados) {
    String fim =
        new String(
            dados,
            Math.max(0, dados.length - 1024),
            Math.min(1024, dados.length),
            StandardCharsets.ISO_8859_1);
    return fim.contains("%%EOF");
  }

  private static boolean textoValido(byte[] dados) {
    try {
      String texto =
          StandardCharsets.UTF_8
              .newDecoder()
              .onMalformedInput(CodingErrorAction.REPORT)
              .onUnmappableCharacter(CodingErrorAction.REPORT)
              .decode(ByteBuffer.wrap(dados))
              .toString();
      return texto
          .codePoints()
          .noneMatch(c -> (c < 32 && c != 9 && c != 10 && c != 13) || c == 127);
    } catch (CharacterCodingException ex) {
      return false;
    }
  }
}
