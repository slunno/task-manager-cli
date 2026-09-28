# Lumeo — configuração e publicação

Marca: **Lumeo**, com a mensagem “Menos interrupções. Mais movimento.”

## Contas de validação

As identidades abaixo foram cadastradas em `helpdesk.usuarios` no projeto Supabase `evsmgbziifqbyahvcwsc` (Projeto de chamados):

| E-mail | Perfil | Uso |
| --- | --- | --- |
| ti.validacao@lumeo.invalid | TI_ADMIN | Administração e atendimento |
| usuario.validacao@lumeo.invalid | FUNCIONARIO | Abertura e acompanhamento de chamados |

São identidades fictícias para o login simulado local do perfil `dev`. Não são contas do Supabase Auth, não possuem senha e não habilitam login online. O login simulado não deve ser publicado em um domínio público.

A conexão local com o banco usa `.env.supabase`, ignorado pelo Git. Consulte [supabase.md](supabase.md) para iniciar o backend e configurar a senha sem incluí-la no código.

## Projeto Vercel

- Conta/equipe: `slunn`.
- Projeto: `lumeo-flow`.
- Identificador: `prj_SmXNRV7opKaseo3tTw2LU5oFeUmL`.
- Domínio atribuído e verificado: `lumeo-flow.vercel.app`.
- Diretório configurado: `frontend`.
- Instalação: `npm ci`.
- Build: `npm run build`.
- Saída: `dist`.

O projeto e o domínio foram criados, mas ainda não foi publicado um deployment. A ligação local fica em `.vercel/project.json`, ignorado pelo Git. `.vercelignore` exclui arquivos de ambiente, dependências e artefatos locais do envio.

## Pendências para publicação funcional

1. Definir o login online: manter o OIDC corporativo existente ou implementar autenticação com e-mail e senha pelo Supabase Auth. O cadastro na tabela da aplicação não substitui uma identidade autenticada.
2. Configurar e publicar o backend Java com acesso ao PostgreSQL. O frontend depende das rotas `/api/v1`; publicar apenas seus arquivos estáticos não disponibiliza o sistema completo.
3. Configurar os requisitos do perfil de produção: autenticação, armazenamento privado de anexos e envio de e-mail, conforme o runbook.
4. Definir a persistência das sessões e a execução das tarefas agendadas de acordo com a hospedagem escolhida. As sessões atuais são mantidas em memória no backend.
5. Verificar login, permissões, abertura de chamados, atendimento e anexos no ambiente publicado.

A Vercel oferece containers em beta, permitindo avaliar a hospedagem do backend Java no mesmo projeto. Containers podem escalar para zero; essa característica precisa ser considerada para sessões e tarefas agendadas. Referências: [Services](https://vercel.com/docs/services) e [Container images](https://vercel.com/docs/functions/container-images).
