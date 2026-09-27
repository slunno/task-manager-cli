# ADR 0006 — Retenção e observabilidade

## Contexto

Chamados contêm dados pessoais em texto, anexos e histórico. A operação precisa de prazo configurável e evidência de falhas de envio, SLA e remoção de objetos, sem expor conteúdo nos logs.

## Decisão

Após o prazo configurado contado do fechamento, um job coordenado por ShedLock anonimiza até 100 chamados por execução. A operação em banco é transacional. O registro do chamado permanece sem conteúdo pessoal para preservar agregados; comentários, histórico, avaliações e metadados de anexos são eliminados. As chaves de objetos entram em fila durável, e a remoção física é tentada após a transação, com nova tentativa no próximo ciclo. A conta técnica inativa mantém a chave estrangeira obrigatória. A aplicação usa SQL parametrizado nesse caso de limpeza transversal, evitando carregar grafos de entidades e respeitando o lote.

Três gauges Micrometer acompanham outbox, SLA vencido e fila de objetos. O filtro de requisições cria ou valida `X-Request-ID` e limpa o contexto ao final. Logs JSON ECS ficam ativos em produção. Métricas exigem TI_ADMIN e devem permanecer atrás de controle de rede.

## Consequências

O processo preserva consistência do banco e permite repetir exclusões físicas após falhas. A remoção de dados é irreversível para o portal; a política e a duração dos backups precisam de decisão organizacional. Uma restauração de backup antigo pode reintroduzir dados e exige nova execução da retenção. O job não substitui um procedimento de atendimento a pedidos individuais dos titulares.
