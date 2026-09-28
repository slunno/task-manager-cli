import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.internal.resolver.ChecksumCalculator;
import org.flywaydb.core.internal.resource.filesystem.FileSystemResource;

/** Exportacao excepcional para um banco vazio pelo MCP, mantendo o historico Flyway real. */
class ExportarBootstrapSupabase {
  public static void main(String[] args) throws Exception {
    Path raiz = Path.of(args[0]).toAbsolutePath();
    Path migrations = raiz.resolve("backend/src/main/resources/db/migration");
    Path destino = raiz.resolve("backend/target/supabase-bootstrap.sql");
    StringBuilder sql = new StringBuilder("""
        -- Gerado das migrations originais. Aplicar somente em banco sem schema helpdesk.
        -- O aplicador deve executar o arquivo inteiro em UMA transacao.
        CREATE SCHEMA helpdesk AUTHORIZATION postgres;
        SET LOCAL search_path TO helpdesk, pg_catalog;
        CREATE TABLE helpdesk.flyway_schema_history (
          installed_rank INTEGER NOT NULL PRIMARY KEY,
          version VARCHAR(50),
          description VARCHAR(200) NOT NULL,
          type VARCHAR(20) NOT NULL,
          script VARCHAR(1000) NOT NULL,
          checksum INTEGER,
          installed_by VARCHAR(100) NOT NULL,
          installed_on TIMESTAMP NOT NULL DEFAULT now(),
          execution_time INTEGER NOT NULL,
          success BOOLEAN NOT NULL
        );
        CREATE INDEX flyway_schema_history_s_idx ON helpdesk.flyway_schema_history (success);
        """);
    StringBuilder manifest = new StringBuilder("version;script;checksum\n");
    try (var arquivos = Files.list(migrations)) {
      var ordenados = arquivos.filter(p -> p.getFileName().toString().matches("V[0-9]+__.+\\.sql"))
          .sorted(Comparator.comparingInt(ExportarBootstrapSupabase::versao)).toList();
      if (ordenados.isEmpty()) throw new IllegalStateException("Nenhuma migration encontrada");
      int rank = 0;
      for (Path arquivo : ordenados) {
        int version = versao(arquivo);
        if (version != ++rank) throw new IllegalStateException("Versoes precisam ser consecutivas");
        String script = arquivo.getFileName().toString();
        String descricao = script.substring(script.indexOf("__") + 2, script.length() - 4).replace('_', ' ');
        var resource = new FileSystemResource(new Location("filesystem:" + migrations),
            arquivo.toString(), StandardCharsets.UTF_8, false);
        // Nao reimplementa CRC32: usa a mesma biblioteca da aplicacao para BOM/encoding/linhas.
        int checksum = ChecksumCalculator.calculate(resource);
        sql.append("\n-- ").append(script).append("\n").append(Files.readString(arquivo));
        sql.append("\nINSERT INTO helpdesk.flyway_schema_history ")
            .append("(installed_rank,version,description,type,script,checksum,installed_by,execution_time,success) VALUES (")
            .append(rank).append(",'").append(version).append("','").append(escapar(descricao))
            .append("','SQL','").append(escapar(script)).append("',").append(checksum)
            .append(",current_user,0,true);\n");
        manifest.append(version).append(';').append(script).append(';').append(checksum).append('\n');
      }
      sql.append(Files.readString(raiz.resolve("scripts/supabase-privacidade.sql")));
      Files.writeString(destino, sql, StandardCharsets.UTF_8);
      Files.writeString(raiz.resolve("backend/target/supabase-migrations.csv"), manifest, StandardCharsets.UTF_8);
      System.out.println("Bootstrap gerado: " + destino + "; migrations: " + rank);
    }
  }

  private static int versao(Path path) {
    String nome = path.getFileName().toString();
    return Integer.parseInt(nome.substring(1, nome.indexOf("__")));
  }

  private static String escapar(String valor) { return valor.replace("'", "''"); }
}
