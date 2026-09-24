# ADR 0003 — Autorização por perfil e recurso no backend

**Status:** aceito em 2026-09-24

## Contexto

Chamados podem conter dados pessoais, anexos e notas internas. Esconder controles no React não restringe chamadas diretas à API.

## Decisão

O backend é a fonte de verdade de permissões. Endpoints de TI e administração usam `@PreAuthorize`. Consultas de funcionário incluem `solicitante_id = usuário autenticado` no predicado; recurso alheio responde `404`. Comentários internos são filtrados na consulta e no mapeamento. Download de anexo verifica o chamado dono. Campos de sistema não constam dos DTOs de entrada de funcionário.

A matriz perfil × endpoint × recurso próprio/alheio será teste de integração obrigatório antes da entrega de cada endpoint. Histórico interno e exportações seguem a mesma política. A UI usa o perfil para apresentar ações, mas nunca substitui os controles da API.

## Consequências

- Métodos de repositório devem aceitar o escopo do ator explicitamente.
- Respostas de erro não devem revelar se um ID alheio existe.
- Revisões de autorização acompanham todo endpoint novo.

