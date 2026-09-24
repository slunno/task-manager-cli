# ADR 0001 — Monólito modular

**Status:** aceito em 2026-09-24

## Contexto

O portal atenderá uma única empresa, com 200 a 500 pessoas. A equipe precisa evoluir identidade, chamados, SLA e notificações de modo independente, sem assumir a operação de vários serviços.

## Decisão

Uma aplicação Spring Boot implantável, organizada por funcionalidade. Cada módulo terá `api`, `application`, `domain` e `infra` quando houver código nessas camadas. Dependências entre módulos passam pela API pública de aplicação ou por eventos; repositórios de um módulo não são injetados em outro. Os módulos previstos são `usuarios`, `chamados`, `comentarios`, `anexos`, `historico`, `notificacoes`, `sla`, `dashboard`, `conhecimento`, `admin` e `compartilhado`.

As regras ficam no domínio e na aplicação. Controllers recebem e validam entrada, delegam e apresentam respostas. Um teste ArchUnit verificará as fronteiras quando os módulos forem implementados.

## Consequências

- Uma transação de banco pode cobrir mudanças de chamado, histórico e outbox.
- Módulos compartilham processo e banco; contratos internos precisam de disciplina e testes.
- Não haverá serviços distribuídos, mensageria externa ou multiempresa.

