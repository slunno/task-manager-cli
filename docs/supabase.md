# Supabase — Projeto de chamados

## Conexao configurada

Projeto `evsmgbziifqbyahvcwsc` (Projeto de chamados), regiao sa-east-1, PostgreSQL 17.
O portal usa PostgreSQL por JDBC no backend Java. A autenticacao e a autorizacao continuam
no Spring Security; nao foram trocadas por Supabase Auth nem por acesso direto do React ao banco.
Nenhuma chave Supabase ou senha precisa ser colocada no frontend.

O perfil adicional `supabase` configura:

- SSL obrigatorio na URL padrao (`sslmode=require`), sem senha embutida.
- Schema privado `helpdesk` em JDBC/Hikari, Hibernate e Flyway.
- Pool Hikari de ate cinco conexoes, minimo de uma, espera maxima de 20 segundos.
- Hibernate em `validate`; Flyway habilitado, sem baseline automatico e com clean desabilitado.
- Uso conjunto com `dev,supabase` localmente ou `prod,supabase` no ambiente de producao.

## Onde colocar a senha

Arquivo local na raiz: **`.env.supabase`**. Foi preparado com senha vazia e e ignorado pelo Git.
Caso ele nao exista em outro checkout, copie [`.env.supabase.example`](../.env.supabase.example).
O `.env` anterior foi preservado.

Preencha somente a variavel abaixo com a senha do banco obtida nas configuracoes do projeto:

```dotenv
SUPABASE_DB_PASSWORD=sua_senha_do_banco
```

O valor fica separado da URL; caracteres como `?`, `&`, `#` e `$` nao precisam de percent-encoding
na senha. O carregador nao executa nem interpola o conteudo. Se houver espacos nas pontas,
envolva o valor em aspas simples ou duplas; somente as aspas externas sao removidas.
Nao envie a senha na conversa nem inclua o arquivo local em commits.

| Variavel | Uso / padrao |
| --- | --- |
| `SUPABASE_DB_PASSWORD` | Obrigatoria; sem valor padrao |
| `SUPABASE_DB_USER` | `postgres` na conexao direta fornecida |
| `SUPABASE_DB_JDBC_URL` | URL direta do exemplo, porta 5432, SSL e schema helpdesk |
| `SUPABASE_DB_POOL_SIZE` | Maximo de conexoes Hikari; padrao 5 |

`postgres` permite o bootstrap, Flyway e o backend atual. Antes de producao, separe a conta de
migrations da conta da aplicacao. Uma conta sem bypass/ownership exige politicas RLS coerentes
com o acesso exclusivo pelo backend; nao crie uma politica generica para usuarios da Data API.

## Iniciar localmente no Windows

Na raiz do projeto, depois de preencher a senha:

```powershell
.\scripts\iniciar-supabase.ps1
```

O script carrega `.env.supabase` apenas no processo, inicia o backend na porta 8080 com
`dev,supabase` e limita o acesso a `127.0.0.1`. Restaura as variaveis do terminal ao encerrar.
Sem senha, encerra com instrucao clara antes de executar Maven ou conectar ao banco.
Para verificar o formato sem iniciar: `.\scripts\iniciar-supabase.ps1 -ValidarConfiguracao`.

Em outro terminal:

```powershell
cd frontend
npm run dev -- --host 127.0.0.1 --port 5173 --strictPort
```

Abra `http://localhost:5173`. Se o terminal tiver `HELPDESK_DEV_API_TARGET` apontando para
a previa simulada na porta 8188, remova essa variavel ou defina `http://127.0.0.1:8080` antes
de iniciar Vite. O primeiro login cria FUNCIONARIO; a promocao de uma identidade especifica
a TI_ADMIN segue o [runbook](runbook.md#primeiro-administrador), usando `helpdesk.usuarios`.

O modo dev simula identidades: use dados ficticios e acesso local. Nao publique dev na internet.
O exemplo suspende a retencao automatica no banco remoto ate a politica ser aprovada.
Anexos locais e MailHog nao passam a usar Supabase Storage ou SMTP remoto por esta configuracao.
Sem SMTP local ativo, os e-mails ficam pendentes/repetem tentativas na outbox; isso nao impede
as operacoes de chamados. Use MailHog/servidor SMTP apropriado para validar entrega.

### IPv6 e Session pooler

A conexao direta fornecida resolve para IPv6. Se a rede local nao tiver IPv6, abra **Connect**
no painel do projeto e copie a URL JDBC do **Session pooler**, porta **5432**, sem credenciais
na URL. Substitua `SUPABASE_DB_JDBC_URL` e `SUPABASE_DB_USER` no arquivo local; o usuario do
pooler inclui o identificador do projeto. Mantenha `sslmode=require` e `currentSchema=helpdesk`.
O hostname do pooler deve ser copiado do painel, nao deduzido a partir da regiao.

Nao use Transaction pooler/porta 6543 para Hibernate/Flyway nesta configuracao.
`sslmode=require` exige criptografia, mas nao verifica CA/hostname. Para producao, obtenha o
certificado CA do painel e configure `verify-full` com `sslrootcert` apropriado ao host.
Veja [Spring Boot no Supabase](https://supabase.com/docs/guides/getting-started/quickstarts/spring-boot)
e [SSL PostgreSQL](https://supabase.com/docs/guides/platform/ssl-enforcement).

## Tabelas criadas

Todas ficam no schema **helpdesk**. No Table Editor, selecione esse schema, em vez de public.

| Area | Tabelas |
| --- | --- |
| Organizacao e pessoas | setores, unidades, usuarios |
| Categorias e SLA | categorias, sla_politicas, expediente, feriados |
| Atendimento | chamados, comentarios, anexos, historico_chamado, avaliacoes |
| E-mails e jobs | notificacoes_outbox, shedlock, storage_exclusao_pendente |
| Conhecimento e produtividade | artigos_conhecimento, respostas_prontas, filtros_salvos |
| Gestao | avisos_incidente |
| Controle de versao | flyway_schema_history |

Sao **19 tabelas da aplicacao e uma de controle**, com PKs, FKs, constraints, indices e sequencias
das migrations V1–V10. Seeds: cinco categorias, quatro politicas SLA e cinco dias de expediente.
Nenhum usuario administrativo, chamado ou credencial foi cadastrado por esta integracao.

## Bootstrap e futuras migrations

Como a senha JDBC sera preenchida depois, o bootstrap inicial foi aplicado pelo plugin Supabase
em uma transacao no projeto remoto vazio. [O exportador](../scripts/ExportarBootstrapSupabase.java)
le as migrations originais sem modifica-las e usa `ChecksumCalculator` da dependencia Flyway
do backend para gravar os checksums na tabela de controle. Os dez registros possuem tipo SQL,
script, versao e checksum correspondente. A execucao foi externa ao Flyway; `execution_time=0`
nao e uma medida de desempenho. A migration do Supabase tambem registra o bootstrap aplicado.

O [perfil](../backend/src/main/resources/application-supabase.yml) aponta o Flyway para essa
mesma tabela no schema privado: no proximo inicio, valida as migrations V1–V10 e aplica novas.
Nao use baseline para esconder divergencias, repair indiscriminado, nem reaplique o bootstrap
no banco existente. Novas mudancas de negocio entram em novas migrations do backend.

Para reproduzir em OUTRO projeto vazio, gere o SQL com `.\scripts\exportar-supabase.ps1`.
Os arquivos gerados ficam em `backend/target/`, fora do Git. Revise o destino e aplique o arquivo
inteiro em uma transacao; ele falha se `helpdesk` ja existir. Nao execute automaticamente no
projeto atual. O gerador usa API interna do Flyway: valide-o novamente ao atualizar a biblioteca.

## Isolamento e verificacao

As vinte tabelas possuem RLS; `anon`, `authenticated` e `service_role` nao possuem USAGE no
schema nem acesso as tabelas/sequencias/funcoes. Grants futuros desses papeis foram revogados
no schema para o dono postgres. Nao exponha helpdesk na Data API. O Spring continua aplicando
as permissoes de FUNCIONARIO/TI_AGENTE/TI_ADMIN; a conta JDBC privilegiada nao fornece RLS por
pessoa. Ao adicionar tabelas ou trocar o dono das migrations, revise RLS e default privileges.

O Advisor informa **RLS sem politicas** nessas tabelas: e intencional, pois clientes Supabase
nao devem acessa-las. Nao adicione politicas permissivas para silenciar esse aviso.
Referencias: [RLS](https://supabase.com/docs/guides/database/postgres/row-level-security) e
[aviso do Advisor](https://supabase.com/docs/guides/database/database-linter?lint=0008_rls_enabled_no_policy).

O Advisor tambem identificou EXECUTE publico na funcao existente `public.rls_auto_enable()`.
Foi confirmado que ela e um event trigger pertencente a postgres; os grants para PUBLIC,
anon e authenticated foram revogados em migration separada, sem modificar a funcao ou o
event trigger. O dono preserva a execucao. [SQL aplicado](../scripts/supabase-event-trigger-privacidade.sql).
Veja [o controle de funcoes privilegiadas](https://supabase.com/docs/guides/database/database-linter?lint=0028_anon_security_definer_function_executable).

A verificacao remota confirma tabelas/seeds/checksums/permissoes. O
[teste SQL transacional](../scripts/verificar-supabase.sql) prova que inserir usuario/chamado
funciona, que UPDATE/DELETE avulso do historico falha e que a retencao pode excluir no contexto
correto; desfaz os dados ficticios. Sequencias de identidade podem avancar durante o teste.
O backend tambem tem testes do perfil Supabase, inclusive senha separada e composicao com prod.

A conexao JDBC autenticada e o fluxo completo da aplicacao com esse banco remoto so podem ser
validados depois que a senha for preenchida. O acesso pelo plugin usa a conexao autorizada do
Supabase e nao exige nem revela a senha JDBC.

### Evidencia em 28/09/2026

- Bootstrap aplicado com sucesso no Projeto de chamados; vinte tabelas, todas com RLS.
- Dez registros Flyway comparados com os checksums oficiais locais, sem divergencia.
- Cinco categorias, quatro politicas e cinco dias de expediente presentes.
- anon/authenticated/service_role sem USAGE no schema e sem SELECT em chamados.
- Verificacao SQL aprovada, com zero fixtures de usuarios/chamados restantes.
- Advisor de seguranca sem WARN/ERROR apos a restricao de EXECUTE; apenas os vinte avisos
  informativos de RLS sem politica, intencionais neste acesso exclusivo por JDBC.
- `mvnw.cmd -B verify`: 83 testes, zero falhas/erros, cinco testes PostgreSQL omitidos localmente
  por ausencia de Docker. Os tres testes novos de configuracao executaram sem skips; cobertura
  e Spotless aprovados. A CI executa os testes PostgreSQL com Docker.
- Carregador PowerShell verificado com senha ficticia e caracteres literais; bloqueia senha
  vazia, SSL opcional e Transaction pooler; restaura o estado original das variaveis do processo.
