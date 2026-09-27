package br.com.empresa.helpdesk.anexos.application;

public interface StorageService {
  void gravar(String chave, byte[] dados, String tipoMime);

  byte[] ler(String chave);

  void remover(String chave);
}
