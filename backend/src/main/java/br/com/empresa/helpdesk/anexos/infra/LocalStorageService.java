package br.com.empresa.helpdesk.anexos.infra;

import br.com.empresa.helpdesk.anexos.application.StorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "helpdesk.storage.mode", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {
  private final Path diretorio;

  public LocalStorageService(
      @Value("${helpdesk.storage.local-directory:./data/anexos}") String diretorio) {
    this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
  }

  private Path destino(String chave) {
    UUID.fromString(chave);
    Path destino = diretorio.resolve(chave).normalize();
    if (!destino.getParent().equals(diretorio))
      throw new IllegalArgumentException("Chave inválida");
    return destino;
  }

  @Override
  public void gravar(String chave, byte[] dados, String tipoMime) {
    try {
      Files.createDirectories(diretorio);
      Files.write(destino(chave), dados, StandardOpenOption.CREATE_NEW);
    } catch (IOException ex) {
      throw new IllegalStateException("Não foi possível armazenar o anexo", ex);
    }
  }

  @Override
  public byte[] ler(String chave) {
    try {
      return Files.readAllBytes(destino(chave));
    } catch (IOException ex) {
      throw new IllegalStateException("Não foi possível ler o anexo", ex);
    }
  }

  @Override
  public void remover(String chave) {
    try {
      Files.deleteIfExists(destino(chave));
    } catch (IOException ex) {
      throw new IllegalStateException("Não foi possível remover o anexo", ex);
    }
  }
}
