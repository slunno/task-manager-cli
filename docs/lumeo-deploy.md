# Lumeo — configuração e publicação

Marca: **Lumeo**, com a mensagem “Menos interrupções. Mais movimento.”

## Contas de validação

As identidades abaixo foram cadastradas em `helpdesk.usuarios` no projeto Supabase `evsmgbziifqbyahvcwsc` (Projeto de chamados):

| E-mail | Perfil | Uso |
| --- | --- | --- |
| ti.validacao@lumeo.invalid | TI_ADMIN | Administração e atendimento |
| usuario.validacao@lumeo.invalid | FUNCIONARIO | Abertura e acompanhamento de chamados |

São identidades fictícias já cadastradas na aplicação. A criação correspondente no Supabase Auth ainda depende da chave secreta do projeto correto. O login simulado não deve ser publicado em um domínio público.

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

1. Criar as contas no Supabase Auth com a chave secreta do projeto correto. Foi escolhido e implementado login com e-mail e senha pelo Supabase; o cadastro na tabela da aplicação não substitui uma identidade autenticada.
2. Configurar e publicar o backend Java com acesso ao PostgreSQL. O frontend depende das rotas `/api/v1`; publicar apenas seus arquivos estáticos não disponibiliza o sistema completo.
3. Configurar os requisitos do perfil de produção: autenticação, armazenamento privado de anexos e envio de e-mail, conforme o runbook.
4. Definir a persistência das sessões e a execução das tarefas agendadas de acordo com a hospedagem escolhida. As sessões atuais são mantidas em memória no backend.
5. Verificar login, permissões, abertura de chamados, atendimento e anexos no ambiente publicado.

A Vercel oferece containers em beta, permitindo avaliar a hospedagem do backend Java no mesmo projeto. Containers podem escalar para zero; essa característica precisa ser considerada para sessões e tarefas agendadas. Referências: [Services](https://vercel.com/docs/services) e [Container images](https://vercel.com/docs/functions/container-images).

## Login com senha implementado

O perfil adicional `supabase-auth` habilita `POST /api/v1/auth/password/login` e desabilita o login simulado, mesmo quando `dev` também está ativo. O backend valida a senha no Supabase usando a chave publicável, exige e-mail confirmado e cadastro ativo na aplicação. O perfil de acesso vem exclusivamente da tabela de usuários do portal. Senhas e tokens do Supabase não são incluídos na sessão da aplicação; a sessão usa cookie HttpOnly e o login exige CSRF.

Para preparar as contas, ajuste no `.env` local:

```dotenv
SUPABASE_URL=https://evsmgbziifqbyahvcwsc.supabase.co
SUPABASE_PUBLISHABLE_KEY=<chave publicável deste projeto>
SUPABASE_SECRET_KEY=<chave secreta deste projeto>
```

Execute `./scripts/criar-contas-validacao.ps1`. O script recusa outra URL antes de qualquer requisição, cria as identidades pela API administrativa e verifica o login. As senhas geradas ficam em `.env.lumeo-validacao`, ignorado pelo Git. Não envie esse arquivo à Vercel nem o compartilhe publicamente.

Para validar localmente com senha, adicione `SUPABASE_URL` e `SUPABASE_PUBLISHABLE_KEY` do projeto correto em `.env.supabase`, mantendo sua configuração de banco. Depois execute:

```powershell
./scripts/iniciar-supabase.ps1 -LoginSupabase
```

O backend não precisa da chave secreta para executar o login. Essa chave fica restrita à administração das contas. O modo de autenticação com senha não elimina os demais requisitos de produção (armazenamento, e-mail e hospedagem do backend).
