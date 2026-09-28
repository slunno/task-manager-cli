package br.com.empresa.helpdesk.anexos.infra;

import br.com.empresa.helpdesk.anexos.application.StorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import java.io.ByteArrayInputStream;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "helpdesk.storage.mode", havingValue = "s3")
public class S3StorageService implements StorageService {
  private final MinioClient client;
  private final String bucket;

  public S3StorageService(
      @Value("${helpdesk.storage.s3.endpoint}") String endpoint,
      @Value("${helpdesk.storage.s3.access-key}") String accessKey,
      @Value("${helpdesk.storage.s3.secret-key}") String secretKey,
      @Value("${helpdesk.storage.s3.bucket}") String bucket) {
    exigir(endpoint, "HELPDESK_S3_ENDPOINT");
    exigir(accessKey, "HELPDESK_S3_ACCESS_KEY");
    exigir(secretKey, "HELPDESK_S3_SECRET_KEY");
    exigir(bucket, "HELPDESK_S3_BUCKET");
    this.client =
        MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    this.bucket = bucket;
  }

  private static void exigir(String valor, String variavel) {
    if (valor == null || valor.isBlank())
      throw new IllegalStateException("Configuração S3 obrigatória: " + variavel);
  }

  private String chaveSegura(String chave) {
    UUID.fromString(chave);
    return chave;
  }

  @Override
  public void gravar(String chave, byte[] dados, String tipoMime) {
    try {
      client.putObject(
          PutObjectArgs.builder().bucket(bucket).object(chaveSegura(chave)).stream(
                  new ByteArrayInputStream(dados), (long) dados.length, -1L)
              .contentType(tipoMime)
              .build());
    } catch (Exception ex) {
      throw new IllegalStateException("Não foi possível armazenar o anexo", ex);
    }
  }

  @Override
  public byte[] ler(String chave) {
    try (var stream =
        client.getObject(
            GetObjectArgs.builder().bucket(bucket).object(chaveSegura(chave)).build())) {
      return stream.readAllBytes();
    } catch (Exception ex) {
      throw new IllegalStateException("Não foi possível ler o anexo", ex);
    }
  }

  @Override
  public void remover(String chave) {
    try {
      client.removeObject(
          RemoveObjectArgs.builder().bucket(bucket).object(chaveSegura(chave)).build());
    } catch (Exception ex) {
      throw new IllegalStateException("Não foi possível remover o anexo", ex);
    }
  }
}
