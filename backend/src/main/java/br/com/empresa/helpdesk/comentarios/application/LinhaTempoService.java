package br.com.empresa.helpdesk.comentarios.application;

import br.com.empresa.helpdesk.chamados.application.ChamadoService;
import br.com.empresa.helpdesk.comentarios.api.LinhaTempoItemResponse;
import br.com.empresa.helpdesk.compartilhado.erros.RequisicaoInvalidaException;
import br.com.empresa.helpdesk.compartilhado.paginacao.PaginaResponse;
import br.com.empresa.helpdesk.usuarios.domain.Usuario;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinhaTempoService {
  private final ChamadoService chamados;
  private final JdbcTemplate jdbc;

  public LinhaTempoService(ChamadoService chamados, JdbcTemplate jdbc) {
    this.chamados = chamados;
    this.jdbc = jdbc;
  }

  @Transactional(readOnly = true)
  public PaginaResponse<LinhaTempoItemResponse> listar(
      Long chamadoId, Usuario ator, int page, int size) {
    chamados.exigirAcesso(chamadoId, ator);
    if (page < 0 || size < 1 || size > 100) {
      throw new RequisicaoInvalidaException("Paginação inválida: page >= 0 e size entre 1 e 100");
    }
    Long total =
        jdbc.queryForObject(
            "SELECT (SELECT count(*) FROM comentarios WHERE chamado_id = ? AND interno = false) "
                + "+ (SELECT count(*) FROM historico_chamado WHERE chamado_id = ? AND campo = 'status')",
            Long.class,
            chamadoId,
            chamadoId);
    List<LinhaTempoItemResponse> itens =
        jdbc.query(
            "SELECT id, tipo, autor_id, texto, status, criado_em FROM ("
                + " SELECT id, 'COMENTARIO' AS tipo, autor_id, texto, CAST(NULL AS VARCHAR(24)) AS status, criado_em"
                + " FROM comentarios WHERE chamado_id = ? AND interno = false"
                + " UNION ALL"
                + " SELECT id, 'STATUS' AS tipo, usuario_id AS autor_id, CAST(NULL AS VARCHAR(10000)) AS texto,"
                + " valor_novo AS status, criado_em"
                + " FROM historico_chamado WHERE chamado_id = ? AND campo = 'status'"
                + ") atividade ORDER BY criado_em ASC, tipo ASC, id ASC LIMIT ? OFFSET ?",
            (rs, rowNum) ->
                new LinhaTempoItemResponse(
                    rs.getLong("id"),
                    rs.getString("tipo"),
                    rs.getObject("autor_id") == null ? null : rs.getLong("autor_id"),
                    rs.getString("texto"),
                    rs.getString("status"),
                    rs.getTimestamp("criado_em").toInstant()),
            chamadoId,
            chamadoId,
            size,
            page * size);
    long quantidade = total == null ? 0 : total;
    return new PaginaResponse<>(
        itens, page, size, quantidade, (int) ((quantidade + size - 1) / size));
  }
}
