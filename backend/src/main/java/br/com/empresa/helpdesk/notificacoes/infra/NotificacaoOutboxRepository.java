package br.com.empresa.helpdesk.notificacoes.infra;

import br.com.empresa.helpdesk.notificacoes.domain.NotificacaoOutbox;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificacaoOutboxRepository extends JpaRepository<NotificacaoOutbox, Long> {
  @Query(
      "select n from NotificacaoOutbox n where n.status in ('PENDENTE', 'FALHA') and n.proximaTentativaEm <= :agora order by n.proximaTentativaEm, n.id")
  List<NotificacaoOutbox> pendentes(Instant agora, Pageable pageable);

  @Query(
      "select n from NotificacaoOutbox n where n.status = 'ENVIANDO' and n.atualizadoEm < :limite")
  List<NotificacaoOutbox> enviosInterrompidos(Instant limite);
}
