# Lumeo no VirtualBox: instalação e acesso pela internet

Roteiro para Windows com processador Intel/AMD, Ubuntu Server 24.04 LTS amd64 e uma VM nova. Conferido com os arquivos do projeto em 30/09/2026. Os comandos Linux abaixo são executados **dentro do Ubuntu**, salvo indicação de PowerShell.

O navegador acessa uma URL HTTPS do Cloudflare, que encaminha ao frontend nginx na VM. O nginx encaminha `/api/` ao backend Java na mesma rede Docker. Banco e autenticação ficam no Supabase; anexos ficam em bucket S3 privado. Frontend e API usam a mesma origem para preservar o login com sessão.

Este roteiro prepara uma demonstração pública. VirtualBox usa seu computador: desligamento, suspensão, queda de internet ou encerramento do túnel interrompem o acesso. Quick Tunnel tem endereço temporário, sem garantia de disponibilidade. A primeira validação inclui o frontend na VM; não usa o endereço Vercel.

## 1. Entender os custos

VirtualBox base, Ubuntu, Docker Engine e Quick Tunnel permitem esta montagem sem mensalidade de servidor. Há consumo de energia, internet e recursos do seu PC. Os serviços externos continuam sujeitos às suas cotas e condições.

O R2 Standard inclui 10 GB-mês, 1 milhão de operações Classe A e 10 milhões Classe B mensais; excedentes são cobrados. A ativação exige assinatura R2 e pode solicitar dados de cobrança. Não é armazenamento ilimitado com bloqueio automático garantido no teto gratuito. Brevo Free inclui 300 envios por dia e exige aprovação/configuração do remetente. Confira também o plano e uso do seu Supabase. Não altere para planos pagos para seguir este roteiro.

Fontes: [R2 preços](https://developers.cloudflare.com/r2/pricing/), [ativação do R2](https://developers.cloudflare.com/r2/get-started/), [Brevo planos](https://help.brevo.com/hc/en-us/articles/208589409-About-Brevo-s-pricing-plans), [Quick Tunnel](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).

## 2. Baixar os instaladores

No Windows:

1. Baixe o instalador **Windows hosts** em [VirtualBox Downloads](https://www.virtualbox.org/wiki/Downloads).
2. Instale o pacote base. O Extension Pack não é necessário para este roteiro.
3. Baixe a ISO **Ubuntu Server 24.04 LTS amd64** em [Ubuntu Releases 24.04](https://releases.ubuntu.com/24.04/). Escolha o arquivo `live-server-amd64.iso` da versão 24.04 disponível.
4. Reserve pelo menos 40 GB livres. Como ponto de partida, use um PC com 16 GB de RAM; com 8 GB pode funcionar, mas feche aplicativos pesados durante o build.

## 3. Criar a máquina virtual

No VirtualBox, clique em **Novo** e configure:

| Campo | Valor |
| --- | --- |
| Nome | Lumeo-Ubuntu |
| ISO | Ubuntu Server baixado |
| Tipo / versão | Linux / Ubuntu 64-bit |
| Instalação automática | Marque pular instalação não assistida, se disponível |
| Memória | 4096 MB |
| Processadores | 2, sem ultrapassar a faixa recomendada para seu PC |
| Disco | VDI, alocação dinâmica, 40 GB |
| Rede / Adaptador 1 | NAT, cabo conectado |

O disco dinâmico cresce conforme o uso, até 40 GB. Se a opção 64-bit não aparecer, verifique se a virtualização Intel VT-x/AMD-V está habilitada no BIOS/UEFI. Não precisa liberar portas no roteador para o túnel.

## 4. Instalar o Ubuntu

1. Inicie a VM e escolha instalar Ubuntu Server.
2. Escolha idioma e teclado.
3. Mantenha DHCP na rede; proxy pode ficar vazio numa rede doméstica sem proxy.
4. Use o disco inteiro **da VM**. Confira o tamanho de 40 GB antes de confirmar.
5. Nome da máquina: `lumeo`. Nome de usuário sugerido: `nathan`. Crie uma senha própria para o Ubuntu.
6. Pule Ubuntu Pro e selecione **Install OpenSSH server**.
7. Não selecione serviços adicionais por snap.
8. Aguarde instalar, reinicie e remova a ISO do drive virtual se o instalador pedir.
9. Entre com usuário e senha. A senha digitada no terminal não mostra caracteres; isso é normal.

Atualize e reinicie:

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y git curl ca-certificates nano openssh-server
sudo systemctl enable --now ssh
sudo reboot
```

## 5. Facilitar o uso pelo terminal do Windows

Este passo é opcional: você também pode usar o console da VM. Para copiar comandos pelo PowerShell:

1. Nas configurações da VM, abra **Rede → Adaptador 1 → Avançado → Redirecionamento de portas**.
2. Adicione uma regra chamada `SSH`: protocolo TCP, IP do hospedeiro `127.0.0.1`, porta do hospedeiro `2222`, IP do convidado vazio, porta do convidado `22`.
3. No PowerShell do Windows, execute:

```powershell
ssh -p 2222 nathan@127.0.0.1
```

Use seu usuário real se escolheu outro. Confirme a impressão digital no primeiro acesso à sua VM e informe a senha Ubuntu. Depois desse login, os comandos desse terminal são executados no Ubuntu. Não crie encaminhamento público para banco ou backend.

## 6. Instalar Docker Engine e Compose

No Ubuntu novo, execute os blocos abaixo. O repositório usa `noble` e `amd64` porque este guia escolheu Ubuntu 24.04 para Intel/AMD.

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<'EOF'
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: noble
Components: stable
Architectures: amd64
Signed-By: /etc/apt/keyrings/docker.asc
EOF
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo docker run --rm hello-world
sudo docker compose version
```

O último comando deve mostrar Compose **2.30 ou superior** (incluindo versões principais posteriores). O arquivo deste roteiro usa `env_file` com formato `raw` para preservar senhas com `$` e outros caracteres. A instalação é baseada na [documentação oficial Docker para Ubuntu](https://docs.docker.com/engine/install/ubuntu/); o requisito do formato está na [referência Compose](https://docs.docker.com/reference/compose-file/services/#env_file).

## 7. Baixar o projeto na VM

```bash
mkdir -p ~/projetos
cd ~/projetos
git clone https://github.com/slunno/task-manager-cli.git
cd task-manager-cli
git switch master
git pull --ff-only
ls compose.virtualbox.yml deploy/virtualbox.env.example
```

Se o repositório solicitar autenticação, use o acesso GitHub autorizado para ele. Se já clonou, entre na pasta e use somente os comandos de atualização. Não instale Java ou Node no Ubuntu: o build usa as imagens dos Dockerfiles.

## 8. Separar as informações do Supabase

No projeto `evsmgbziifqbyahvcwsc`, abra **Connect** e copie o host e usuário do **Session pooler**, porta **5432**. Use os valores mostrados no painel, pois o host pode variar. Não use Transaction pooler 6543 neste roteiro.

Monte a URL JDBC assim, substituindo somente o host:

```text
jdbc:postgresql://HOST_DO_SESSION_POOLER:5432/postgres?sslmode=require&currentSchema=helpdesk
```

A senha atual do banco vai em `SUPABASE_DB_PASSWORD`, separada da URL. Em **Settings → API Keys**, obtenha a chave **publishable** para `SUPABASE_PUBLISHABLE_KEY`. `SUPABASE_URL` é `https://evsmgbziifqbyahvcwsc.supabase.co`. Os endereços `/oauth/authorize`, `/oauth/token` e `/jwks.json` enviados anteriormente não substituem essa URL base.

Este runtime não precisa de `SUPABASE_SECRET_KEY` nem `service_role`. Mantenha o schema `helpdesk` fora dos schemas expostos da Data API; o backend acessa por JDBC. Não copie senhas para arquivos versionados.

Fontes: [conexões Postgres](https://supabase.com/docs/guides/database/connecting-to-postgres), [chaves API](https://supabase.com/docs/guides/api/api-keys).

## 9. Configurar os anexos no R2

Se já possui S3 compatível, use suas credenciais. Para R2:

1. Abra o painel Cloudflare → **R2 Object Storage**.
2. Leia as condições de ativação e cobrança antes de ativar.
3. Crie um bucket chamado `lumeo-anexos`, classe **Standard**.
4. Mantenha acesso público/r2.dev desativado; downloads passam pelo backend autenticado.
5. Abra a área de tokens R2 e crie um token com **Object Read & Write**, limitado ao bucket `lumeo-anexos`.
6. Guarde **Access Key ID**, **Secret Access Key** e **S3 API Endpoint** exibidos. Use essas credenciais S3, não o valor genérico de API token.
7. Copie para `HELPDESK_S3_ACCESS_KEY`, `HELPDESK_S3_SECRET_KEY` e `HELPDESK_S3_ENDPOINT`. O endpoint costuma ter formato `https://ACCOUNT_ID.r2.cloudflarestorage.com`; use o fornecido pelo painel.
8. `HELPDESK_S3_BUCKET=lumeo-anexos`. Acompanhe uso no painel durante a demonstração.

Veja [integração S3 oficial do R2](https://developers.cloudflare.com/r2/get-started/s3/).

## 10. Configurar e-mails reais

Um exemplo que oferece cota gratuita é Brevo:

1. Crie a conta Free e conclua as verificações solicitadas.
2. Cadastre e verifique um remetente. Se o provedor exigir autenticação do domínio, conclua essa configuração ou use um remetente permitido para teste.
3. Abra **SMTP & API** e copie o login SMTP exibido.
4. Gere uma chave **SMTP**. Ela será `SMTP_PASSWORD`; não é a senha de login da conta nem uma chave API HTTP.
5. Configure host `smtp-relay.brevo.com`, porta `587`, autenticação e os dois campos STARTTLS em `true`.
6. `HELPDESK_MAIL_FROM` deve conter o e-mail do remetente verificado.

Não use `localhost:1025` e autenticação desativada nesta configuração pública. O projeto exige SMTP autenticado com STARTTLS em produção. O healthcheck não verifica entrega de e-mails; ela será testada depois.

Fonte: [configuração SMTP Brevo](https://help.brevo.com/hc/en-us/articles/7924908994450-Send-transactional-emails-using-Brevo-SMTP).

## 11. Criar o arquivo privado de configuração

Na pasta do projeto dentro do Ubuntu:

```bash
cp deploy/virtualbox.env.example .env.virtualbox
chmod 600 .env.virtualbox
nano .env.virtualbox
```

Preencha **todos** os valores que começam com `PREENCHER`. Os nomes e exemplos estão no arquivo. Use `CHAVE=valor`, sem aspas e sem espaços nas extremidades. Para este arquivo `raw`, aspas viram parte do valor. Preserve os caracteres reais das senhas; não escape `$`. Não execute `source .env.virtualbox`.

Mantenha `HELPDESK_PORTAL_URL=https://example.invalid` apenas enquanto obtém o endereço do túnel. Depois será obrigatório substituí-lo antes de testar chamados/e-mails. Salve no nano com **Ctrl+O**, Enter e **Ctrl+X**.

Confira se sobraram campos, mostrando apenas seus nomes:

```bash
awk -F= '/^[^#][^=]*=.*PREENCHER/ {print "Preencher: " $1}' .env.virtualbox
git check-ignore .env.virtualbox
```

O primeiro comando não deve retornar nada. O segundo deve mostrar `.env.virtualbox`: ele é ignorado pelo Git. Não use `docker compose config` sem `--quiet`, pois a saída completa pode expor credenciais.

## 12. Subir e verificar os contêineres

```bash
sudo docker compose -f compose.virtualbox.yml config --quiet
sudo docker compose -f compose.virtualbox.yml up -d --build
sudo docker compose -f compose.virtualbox.yml ps
curl --fail http://127.0.0.1:3000/actuator/health
```

O primeiro build pode demorar vários minutos. Os dois serviços devem ficar `healthy`; o health deve retornar `UP`. A primeira inicialização executa as migrações pendentes no banco indicado. Use o banco de teste e mantenha uma cópia de segurança se ele já contém dados relevantes.

Para diagnosticar falhas:

```bash
sudo docker compose -f compose.virtualbox.yml logs --tail=100 backend
sudo docker compose -f compose.virtualbox.yml logs --tail=100 frontend
```

Leia localmente antes de compartilhar logs. Não publique dumps de configurações. Use sempre `-f compose.virtualbox.yml`: o Compose padrão é de desenvolvimento e não deve receber acesso público.

## 13. Abrir o túnel HTTPS

Instale o `cloudflared` amd64 na VM. A versão 2026.9.3 estava disponível na [página oficial de releases](https://github.com/cloudflare/cloudflared/releases/tag/2026.9.3) quando este guia foi escrito:

```bash
curl -fL https://github.com/cloudflare/cloudflared/releases/download/2026.9.3/cloudflared-linux-amd64.deb -o /tmp/cloudflared-lumeo.deb
sudo dpkg -i /tmp/cloudflared-lumeo.deb
cloudflared --version
cloudflared tunnel --url http://127.0.0.1:3000
```

Mantenha esse terminal aberto. Copie a URL gerada, parecida com `https://palavras-aleatorias.trycloudflare.com`. Abra uma **segunda sessão SSH** pelo PowerShell e atualize:

```bash
cd ~/projetos/task-manager-cli
nano .env.virtualbox
```

Troque `HELPDESK_PORTAL_URL` pela URL real, sem barra final. Salve e aplique:

```bash
sudo docker compose -f compose.virtualbox.yml up -d --force-recreate backend frontend
sudo docker compose -f compose.virtualbox.yml ps
```

Recriar frontend junto com backend permite renovar a resolução do serviço pelo nginx. Depois, no navegador Windows, abra **a URL HTTPS gerada**. Não use `localhost:5173` ou HTTP da VM para validar esse login: os cookies de produção exigem HTTPS.

O Quick Tunnel não exige domínio próprio; seu endereço muda quando reiniciado. Se existir `~/.cloudflared/config.yaml`, ele pode impedir Quick Tunnel: use uma instalação sem configuração de túnel nomeado para esse teste. Consulte a [documentação Quick Tunnel](https://developers.cloudflare.com/tunnel/get-started/quick-tunnels/).

## 14. Conferir as duas contas e testar o portal

Uma identidade no Supabase Auth e um cadastro no portal são coisas separadas. Não considere as contas criadas apenas porque existem registros em uma tabela.

1. No Supabase, abra **Authentication → Users** e verifique as contas de teste. Se faltarem, crie usuários com e-mails que você controla, senha de teste e e-mail confirmado pela opção administrativa disponível. Não altere identidades diretamente por SQL.
2. No SQL Editor, consulte os cadastros existentes:

```sql
SELECT id, nome, email, perfil, ativo
FROM helpdesk.usuarios
ORDER BY id;
```

3. O e-mail deve coincidir com o da identidade Auth, e `ativo` deve ser `true`. A conta TI precisa de `TI_ADMIN`; a normal, `FUNCIONARIO`.
4. Se não houver cadastros correspondentes, use o cadastro administrativo do portal quando já tiver um administrador. Para inicializar as duas contas numa base de teste sem administrador, substitua os e-mails abaixo pelos mesmos criados no Auth e execute no SQL Editor administrativo:

```sql
INSERT INTO helpdesk.usuarios (nome, email, perfil, ativo)
SELECT 'TI Validação', 'SEU_EMAIL_TI', 'TI_ADMIN', true
WHERE NOT EXISTS (
  SELECT 1 FROM helpdesk.usuarios WHERE lower(email) = lower('SEU_EMAIL_TI')
);

INSERT INTO helpdesk.usuarios (nome, email, perfil, ativo)
SELECT 'Usuário Validação', 'SEU_EMAIL_USUARIO', 'FUNCIONARIO', true
WHERE NOT EXISTS (
  SELECT 1 FROM helpdesk.usuarios WHERE lower(email) = lower('SEU_EMAIL_USUARIO')
);
```

Esse SQL cria somente cadastros ausentes, sem mudar o perfil de contas existentes. Se já houver um cadastro com outro perfil, ajuste explicitamente pelo administrador após conferir o registro. Senhas ficam no Auth e não nessas tabelas.

Teste pelo endereço HTTPS:

1. Entre como usuário normal e abra um chamado.
2. Anexe um PDF/PNG pequeno e confirme que ele aparece no bucket privado.
3. Saia e entre como TI; abra a fila, atribua, responda e altere o status.
4. Volte como usuário normal e confira resposta e download do anexo.
5. Confira recebimento das notificações e os logs transacionais no provedor SMTP.
6. Tente acessar a área administrativa como usuário normal; ela deve ser negada.
7. Recarregue a página e confirme manutenção da sessão. Teste sair também.

Não declare a publicação concluída apenas porque a tela de login abriu. `healthy` comprova inicialização, mas não comprova upload S3, entrega SMTP nem permissões.

## 15. Operar, atualizar e desligar

Todos estes comandos são no Ubuntu, na pasta do projeto. Status e logs:

```bash
sudo docker compose -f compose.virtualbox.yml ps
sudo docker compose -f compose.virtualbox.yml logs --tail=100 backend
```

Após um commit novo, atualize numa janela em que possa interromper os testes:

```bash
git pull --ff-only
sudo docker compose -f compose.virtualbox.yml up -d --build --force-recreate backend frontend
```

Reiniciar backend encerra sessões atuais; faça login novamente. Para parar o acesso público, pressione **Ctrl+C** no terminal do túnel. Para parar os serviços:

```bash
sudo docker compose -f compose.virtualbox.yml down
sudo shutdown -h now
```

Isso não apaga o banco Supabase nem objetos S3. Para retomar, ligue a VM, confira os contêineres, inicie o túnel novamente e repita a atualização de `HELPDESK_PORTAL_URL` com o novo endereço. `restart: unless-stopped` ajuda a retomar contêineres após reiniciar a VM, mas não inicia a própria VM nem o túnel.

Desative suspensão automática do Windows enquanto estiver hospedando; mantenha a VM ligada e o túnel aberto. Faça backup dos dados remotos e guarde uma cópia protegida do `.env.virtualbox`, que contém credenciais. Um snapshot da VM também pode conter esses segredos.

## 16. Resolver os problemas mais comuns

| Sintoma | Verificação / ação |
| --- | --- |
| VirtualBox não mostra Ubuntu 64-bit | Verifique virtualização no BIOS/UEFI e compatibilidade do host. |
| SSH recusado | VM ligada, NAT, regra 127.0.0.1:2222 para 22 e `sudo systemctl status ssh` na VM. |
| Compose rejeita `format: raw` | Atualize o plugin oficial; precisa de Compose >= 2.30. |
| Backend unhealthy | Leia logs; confirme DB, schema, perfis e campos obrigatórios S3/SMTP/Auth. |
| Falha de autenticação Postgres | Use senha atual do banco e usuário/host do Session pooler, sem aspas/espaços. |
| DNS/rede do banco falha | Use Session pooler IPv4, porta 5432 e confira acesso de saída da rede/VM. |
| Erro Flyway | Confirme banco/schema e leia migração indicada; não apague tabelas nem histórico para forçar inicialização. |
| Página funciona, mas login volta à tela inicial | Acesse HTTPS do túnel; confira conta Auth, publishable key e cookies. |
| Login válido sem perfil correto | Confira e-mail/perfil/ativo em `helpdesk.usuarios`; Auth sozinho não concede TI_ADMIN. |
| HTTP 429 | Aguarde a janela de limite (padrão 1 minuto). Não desative rate limiting para publicar. O proxy pode agrupar IPs neste teste. |
| Upload falha | Bucket privado existente, endpoint S3 correto, token com escrita e arquivo dentro do limite. |
| E-mail não chega | Verifique remetente, credencial SMTP, STARTTLS e cota. Health não testa SMTP. |
| Cloudflare 502 | Confira health em 127.0.0.1:3000 e se o túnel está rodando dentro da mesma VM. |
| Túnel não conecta | Confira acesso de saída da rede; firewalls corporativos podem bloquear o conector. Não abra banco/backend para contornar. |
| Build termina por memória/disco | Confira `free -h` e `df -h`; aumente RAM/disco da VM e feche apps pesados no host. |
| URL antiga deixou de funcionar | Reinicie túnel, copie a nova URL e atualize portal URL com recriação dos dois serviços. |

Este guia e o Compose precisam de validação na VM criada. O ambiente Windows onde foram preparados não possui Docker instalado; nenhum deploy público foi executado somente com a criação destes arquivos.
