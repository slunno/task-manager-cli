package br.com.empresa.helpdesk.anexos;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.params.provider.Arguments;

public final class ArquivosAnexoTeste {
  private ArquivosAnexoTeste() {}

  public static Stream<Arguments> amostras() {
    return Stream.of(
        Arguments.of(
            "gif",
            "image/gif",
            Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7")),
        Arguments.of(
            "webp",
            "image/webp",
            Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA")),
        Arguments.of(
            "csv", "text/csv", "data;evento\n2026-09-28;falha\n".getBytes(StandardCharsets.UTF_8)),
        Arguments.of(
            "log", "text/plain", "2026-09-28 INFO diagnóstico\n".getBytes(StandardCharsets.UTF_8)),
        Arguments.of(
            "zip",
            "application/zip",
            zip(Map.of("diagnostico.txt", "Teste".getBytes(StandardCharsets.UTF_8)))),
        Arguments.of(
            "docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            office("docx")),
        Arguments.of(
            "xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            office("xlsx")));
  }

  public static byte[] office(String ext) {
    String principal = ext.equals("docx") ? "word/document.xml" : "xl/workbook.xml";
    String tipo =
        ext.equals("docx")
            ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"
            : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";
    String tipos =
        "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Override PartName=\"/"
            + principal
            + "\" ContentType=\""
            + tipo
            + "\"/></Types>";
    String rels =
        "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\""
            + principal
            + "\"/></Relationships>";
    String corpo =
        ext.equals("docx")
            ? "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body/></w:document>"
            : "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheets/></workbook>";
    return zip(
        Map.of(
            "[Content_Types].xml",
            tipos.getBytes(StandardCharsets.UTF_8),
            "_rels/.rels",
            rels.getBytes(StandardCharsets.UTF_8),
            principal,
            corpo.getBytes(StandardCharsets.UTF_8)));
  }

  public static byte[] zip(Map<String, byte[]> entradas) {
    try {
      var saida = new ByteArrayOutputStream();
      try (var zip = new ZipOutputStream(saida)) {
        for (var entrada : entradas.entrySet()) {
          zip.putNextEntry(new ZipEntry(entrada.getKey()));
          zip.write(entrada.getValue());
          zip.closeEntry();
        }
      }
      return saida.toByteArray();
    } catch (java.io.IOException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
