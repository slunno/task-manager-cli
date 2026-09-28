package br.com.empresa.helpdesk.anexos.application;

import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

final class ValidacaoArquivoZip {
  private static final int MAX_ENTRADAS = 1000;
  private static final long MAX_TOTAL = 50L * 1024 * 1024;
  private static final long MAX_ENTRADA = 20L * 1024 * 1024;
  private static final Set<String> BLOQUEADOS =
      Set.of(
          "exe", "com", "dll", "msi", "scr", "bat", "cmd", "sh", "ps1", "js", "html", "htm", "svg",
          "jar", "vbs", "vbe", "wsf", "hta", "php", "py", "rb", "pl", "lnk", "url", "zip", "rar",
          "7z", "gz", "tgz", "docm", "xlsm", "docx", "xlsx");

  private ValidacaoArquivoZip() {}

  static void validar(byte[] dados, String extensao) {
    int quantidade = validarDiretorioCentral(dados);
    Set<String> nomes = new HashSet<>();
    Set<String> normalizados = new HashSet<>();
    byte[] tipos = null;
    long total = 0;
    try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(dados))) {
      java.util.zip.ZipEntry entrada;
      byte[] buffer = new byte[8192];
      while ((entrada = zip.getNextEntry()) != null) {
        String nome = entrada.getName();
        validarNome(nome);
        if (!nomes.add(nome)
            || !normalizados.add(nome.toLowerCase(Locale.ROOT))
            || nomes.size() > MAX_ENTRADAS) rejeitar();
        boolean conteudoTipos = nome.equals("[Content_Types].xml");
        var xml = conteudoTipos ? new ByteArrayOutputStream() : null;
        long tamanho = 0;
        int lidos;
        while ((lidos = zip.read(buffer)) != -1) {
          tamanho += lidos;
          total += lidos;
          if (tamanho > MAX_ENTRADA
              || total > MAX_TOTAL
              || (total > 1024 * 1024 && total > dados.length * 100L)) rejeitar();
          if (conteudoTipos) {
            if (tamanho > 256 * 1024) rejeitar();
            xml.write(buffer, 0, lidos);
          }
        }
        if (conteudoTipos) tipos = xml.toByteArray();
        zip.closeEntry();
      }
      if (nomes.isEmpty() || nomes.size() != quantidade) rejeitar();
      if (!"zip".equals(extensao)) validarOffice(nomes, tipos, extensao);
    } catch (RequisicaoInvalidaException ex) {
      throw ex;
    } catch (java.io.IOException ex) {
      throw new RequisicaoInvalidaException("ZIP inválido ou estrutura não suportada");
    }
  }

  private static int validarDiretorioCentral(byte[] dados) {
    var buffer = java.nio.ByteBuffer.wrap(dados).order(java.nio.ByteOrder.LITTLE_ENDIAN);
    int fim = -1;
    for (int i = dados.length - 22; i >= Math.max(0, dados.length - 65557); i--) {
      if (buffer.getInt(i) == 0x06054b50
          && Short.toUnsignedInt(buffer.getShort(i + 20)) == dados.length - i - 22) {
        fim = i;
        break;
      }
    }
    if (fim < 0) rejeitar();
    int quantidade = Short.toUnsignedInt(buffer.getShort(fim + 10));
    long tamanho = Integer.toUnsignedLong(buffer.getInt(fim + 12));
    long inicio = Integer.toUnsignedLong(buffer.getInt(fim + 16));
    if (buffer.getShort(fim + 4) != 0
        || buffer.getShort(fim + 6) != 0
        || quantidade == 0
        || quantidade > MAX_ENTRADAS
        || Short.toUnsignedInt(buffer.getShort(fim + 8)) != quantidade
        || inicio + tamanho != fim) rejeitar();
    int cursor = (int) inicio;
    for (int i = 0; i < quantidade; i++) {
      if (cursor < 0
          || cursor + 46 > fim
          || buffer.getInt(cursor) != 0x02014b50
          || (buffer.getShort(cursor + 8) & 1) != 0
          || buffer.getShort(cursor + 34) != 0) rejeitar();
      int modo = buffer.getInt(cursor + 38) >>> 16;
      if ((modo & 0xf000) == 0xa000) rejeitar();
      cursor +=
          46
              + Short.toUnsignedInt(buffer.getShort(cursor + 28))
              + Short.toUnsignedInt(buffer.getShort(cursor + 30))
              + Short.toUnsignedInt(buffer.getShort(cursor + 32));
    }
    if (cursor != fim) rejeitar();
    return quantidade;
  }

  private static void validarNome(String nome) {
    String limpo = nome.toLowerCase(Locale.ROOT);
    if (nome.isBlank()
        || nome.length() > 255
        || nome.startsWith("/")
        || nome.contains("\\")
        || nome.contains(":")
        || nome.codePoints().anyMatch(c -> c < 32 || c == 127)
        || java.util.Arrays.asList(nome.split("/")).contains("..")
        || limpo.contains("vbaproject")
        || BLOQUEADOS.contains(limpo.substring(limpo.lastIndexOf('.') + 1))) rejeitar();
  }

  private static void validarOffice(Set<String> nomes, byte[] tipos, String extensao) {
    String principal = "docx".equals(extensao) ? "word/document.xml" : "xl/workbook.xml";
    String esperado =
        "docx".equals(extensao)
            ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"
            : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";
    if (tipos == null || !nomes.contains(principal) || !nomes.contains("_rels/.rels")) rejeitar();
    try {
      var factory = DocumentBuilderFactory.newInstance();
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
      factory.setNamespaceAware(true);
      var builder = factory.newDocumentBuilder();
      builder.setErrorHandler(
          new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void fatalError(org.xml.sax.SAXParseException ex)
                throws org.xml.sax.SAXException {
              throw ex;
            }
          });
      var documento = builder.parse(new ByteArrayInputStream(tipos));
      var entries =
          documento.getElementsByTagNameNS(
              "http://schemas.openxmlformats.org/package/2006/content-types", "Override");
      boolean valido = false;
      for (int i = 0; i < entries.getLength(); i++) {
        var elemento = (org.w3c.dom.Element) entries.item(i);
        String tipo = elemento.getAttribute("ContentType");
        if (tipo.toLowerCase(Locale.ROOT).contains("macroenabled")) rejeitar();
        if (elemento.getAttribute("PartName").equals("/" + principal) && tipo.equals(esperado))
          valido = true;
      }
      if (!valido) rejeitar();
    } catch (RequisicaoInvalidaException ex) {
      throw ex;
    } catch (javax.xml.parsers.ParserConfigurationException
        | org.xml.sax.SAXException
        | java.io.IOException ex) {
      throw new RequisicaoInvalidaException("Documento Office inválido ou estrutura não suportada");
    }
  }

  private static void rejeitar() {
    throw new RequisicaoInvalidaException(
        "Arquivo compactado com estrutura ou tamanho não permitido");
  }
}
