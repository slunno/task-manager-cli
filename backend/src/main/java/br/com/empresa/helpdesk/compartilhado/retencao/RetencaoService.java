package br.com.empresa.helpdesk.compartilhado.retencao;

import br.com.empresa.helpdesk.anexos.application.StorageService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetencaoService {
  private static final Logger LOG = LoggerFactory.getLogger(RetencaoService.class);
  private static final String EMAIL_ANONIMO = "anonimo@helpdesk.invalid";
  private final JdbcTemplate jdbc;
  private final StorageService storage;
  private final Clock clock;
  private final int anos;

  public RetencaoService(
      JdbcTemplate jdbc,
      StorageService storage,
      Clock clock,
      @Value("${helpdesk.retencao.anos:3}") int anos) {
    if (anos < 1 || anos > 30)
      throw new IllegalArgumentException("Retenção deve ser de 1 a 30 anos");
    this.jdbc = jdbc;
    this.storage = storage;
    this.clock = clock;
    this.anos = anos;
  }

  @Transactional
  public int anonimizarLote() {
    Instant agora = Instant.now(clock);
    Instant limite = agora.atZone(ZoneId.of("America/Sao_Paulo")).minusYears(anos).toInstant();
    List<Long> ids =
        jdbc.query(
            "select id from chamados where status = 'FECHADO' and fechado_em < ? and anonimizado_em is null order by fechado_em, id limit 100",
            (rs, i) -> rs.getLong(1),
            Timestamp.from(limite));
    if (!ids.isEmpty()) {
      contextoRetencao(true);
      Long anonimoId = usuarioAnonimo(agora);
      for (Long id : ids) {
        List<String> chaves =
            jdbc.query(
                "select chave_storage from anexos where chamado_id = ?",
                (rs, i) -> rs.getString(1),
                id);
        for (String chave : chaves)
          jdbc.update("insert into storage_exclusao_pendente (chave_storage) values (?)", chave);
        jdbc.update("delete from anexos where chamado_id = ?", id);
        jdbc.update("delete from comentarios where chamado_id = ?", id);
        jdbc.update("delete from historico_chamado where chamado_id = ?", id);
        jdbc.update("delete from avaliacoes where chamado_id = ?", id);
        jdbc.update(
            "update chamados set chamado_principal_id = null, version = version + 1 where chamado_principal_id = ?",
            id);
        jdbc.update(
            "update chamados set titulo = 'Chamado anonimizado', descricao = 'Conteúdo removido por retenção', solucao = null, solicitante_id = ?, aberto_por_id = null, responsavel_id = null, prioridade_sugerida = null, chamado_principal_id = null, anonimizado_em = ?, atualizado_em = ?, version = version + 1 where id = ?",
            anonimoId,
            Timestamp.from(agora),
            Timestamp.from(agora),
            id);
      }
      contextoRetencao(false);
    }
    jdbc.update("delete from notificacoes_outbox where criado_em < ?", Timestamp.from(limite));
    return ids.size();
  }

  private void contextoRetencao(boolean ativo) {
    jdbc.execute(
        (ConnectionCallback<Void>)
            conexao -> {
              if ("PostgreSQL".equals(conexao.getMetaData().getDatabaseProductName())) {
                if (conexao.getAutoCommit())
                  throw new IllegalStateException("Retenção exige transação ativa");
                try (var comando =
                    conexao.prepareStatement("select set_config('helpdesk.retencao', ?, true)")) {
                  comando.setString(1, ativo ? "on" : "off");
                  comando.execute();
                }
              }
              return null;
            });
  }

  public int limparObjetosPendentes() {
    List<String> chaves =
        jdbc.query(
            "select chave_storage from storage_exclusao_pendente order by criado_em limit 100",
            (rs, i) -> rs.getString(1));
    int removidos = 0;
    for (String chave : chaves) {
      try {
        storage.remover(chave);
        jdbc.update("delete from storage_exclusao_pendente where chave_storage = ?", chave);
        removidos++;
      } catch (RuntimeException ex) {
        LOG.warn("Falha ao remover anexo da fila de retenção; será tentado novamente");
      }
    }
    return removidos;
  }

  private Long usuarioAnonimo(Instant agora) {
    List<Long> ids =
        jdbc.query(
            "select id from usuarios where email = ?", (rs, i) -> rs.getLong(1), EMAIL_ANONIMO);
    if (!ids.isEmpty()) return ids.getFirst();
    jdbc.update(
        "insert into usuarios (nome, email, perfil, ativo, criado_em, atualizado_em) values ('Usuário anonimizado', ?, 'FUNCIONARIO', false, ?, ?)",
        EMAIL_ANONIMO,
        Timestamp.from(agora),
        Timestamp.from(agora));
    return jdbc.queryForObject(
        "select id from usuarios where email = ?", Long.class, EMAIL_ANONIMO);
  }
}
