package br.com.empresa.helpdesk.anexos.application;

import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public final class ConteudoAnexo {
  static final long LIMITE = 10L * 1024 * 1024;
  private static final Map<String, String> TIPOS =
      Map.ofEntries(
          Map.entry("pdf", "application/pdf"),
          Map.entry("png", "image/png"),
          Map.entry("jpg", "image/jpeg"),
          Map.entry("jpeg", "image/jpeg"),
          Map.entry("txt", "text/plain"),
          Map.entry("gif", "image/gif"),
          Map.entry("webp", "image/webp"),
          Map.entry("csv", "text/csv"),
          Map.entry("log", "text/plain"),
          Map.entry("zip", "application/zip"),
          Map.entry(
              "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
          Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
  private final Set<String> permitidos;

  public ConteudoAnexo(
      @Value(
              "${helpdesk.anexos.tipos-permitidos:pdf,png,jpg,jpeg,txt,gif,webp,csv,log,docx,xlsx,zip}")
          String tipos) {
    permitidos =
        Set.copyOf(
            java.util.Arrays.stream(tipos.split(","))
                .map(String::trim)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .toList());
    if (permitidos.isEmpty() || !TIPOS.keySet().containsAll(permitidos))
      throw new IllegalArgumentException(
          "HELPDESK_ATTACHMENT_TYPES deve conter apenas extensões suportadas e seguras");
  }

  public String validar(byte[] dados, String declarado, String nome) {
    if (dados.length == 0 || dados.length > LIMITE) {
      throw new RequisicaoInvalidaException("Anexo deve ter entre 1 byte e 10 MB");
    }
    String informado =
        declarado == null ? "" : declarado.toLowerCase(Locale.ROOT).split(";", 2)[0].trim();
    String extensao = nome.substring(nome.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    String mime = TIPOS.get(extensao);
    boolean declaradoValido =
        mime != null
            && (mime.equals(informado)
                || (extensao.equals("csv")
                    && Set.of("text/plain", "application/csv").contains(informado))
                || (extensao.equals("log") && informado.equals("application/octet-stream"))
                || (extensao.equals("zip") && informado.equals("application/x-zip-compressed")));
    if (!permitidos.contains(extensao) || !declaradoValido || !compativel(dados, mime)) {
      throw new RequisicaoInvalidaException("Formato de anexo não permitido ou conteúdo inválido");
    }
    if (Set.of("zip", "docx", "xlsx").contains(extensao))
      ValidacaoArquivoZip.validar(dados, extensao);
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
      case "text/plain", "text/csv" -> textoValido(dados);
      case "image/gif" ->
          dados.length >= 14
              && (comeca(dados, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                  || comeca(dados, "GIF89a".getBytes(StandardCharsets.US_ASCII)))
              && dados[dados.length - 1] == 0x3b;
      case "image/webp" ->
          dados.length >= 20
              && comeca(dados, "RIFF".getBytes(StandardCharsets.US_ASCII))
              && new String(dados, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")
              && Set.of("VP8 ", "VP8L", "VP8X")
                  .contains(new String(dados, 12, 4, StandardCharsets.US_ASCII))
              && Integer.toUnsignedLong(
                      ByteBuffer.wrap(dados, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt())
                  == dados.length - 8L;
      case "application/zip",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ->
          comeca(dados, new byte[] {80, 75, 3, 4});
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
