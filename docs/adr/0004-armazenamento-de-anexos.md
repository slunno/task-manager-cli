# ADR 0004 — Armazenamento de anexos

**Status:** aceito em 2026-09-24

## Contexto

Anexos variam conforme o ambiente. O banco deve guardar metadados, não arquivos grandes. O nome enviado pelo usuário não pode virar caminho de armazenamento.

## Decisão

`StorageService` abstrairá gravação, leitura e remoção. Desenvolvimento usará volume em disco; produção usará S3 compatível, inclusive MinIO. O backend gerará chave aleatória, imporá tamanho e lista de MIME, validará o conteúdo e manterá o nome original só como metadado. Upload e download passam por autorização sobre o chamado. Objetos não serão públicos.

## Consequências

- Banco e storage não têm transação distribuída: falhas parciais exigem limpeza e reconciliação.
- A configuração de bucket/volume e a rotina de backup fazem parte do runbook.
- Não serão geradas URLs públicas permanentes.

