# ADR 0005 — Outbox transacional de notificações

**Status:** aceito em 2026-09-24

## Contexto

Uma falha de SMTP não pode desfazer a abertura do chamado nem perder definitivamente o aviso.

## Decisão

Eventos que pedem e-mail serão persistidos em `notificacoes_outbox` na mesma transação da ação de negócio. Um job `@Scheduled`, coordenado por ShedLock em múltiplas instâncias, buscará lotes pendentes. Cada tentativa registrará estado e próxima data com backoff exponencial limitado. A mensagem deve ser idempotente para reduzir duplicações em reprocessamentos. Usuários inativos não receberão novas notificações.

## Consequências

- Entrega será eventual; a UI não deve depender do envio imediato.
- Operação precisa de métricas para pendências, falhas e idade da fila.
- O envio de e-mail não ocorre dentro da requisição HTTP. Falhas de SMTP não derrubam a saúde HTTP; a fila pendente terá alerta próprio.
