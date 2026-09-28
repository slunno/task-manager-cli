package br.com.empresa.helpdesk.anexos.application;

/** Extensão para antivírus; implementações rejeitam lançando RequisicaoInvalidaException. */
@FunctionalInterface
public interface ScannerAnexo {
  void verificar(byte[] dados, String tipoMime);
}
