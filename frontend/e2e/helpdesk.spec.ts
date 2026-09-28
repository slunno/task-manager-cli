import { expect, test, type BrowserContext, type Page } from '@playwright/test'

test.describe.serial('Atendimento real, privacidade e conclusão', () => {
  let empregado: BrowserContext
  let ti: BrowserContext
  let solicitante: Page
  let agente: Page
  let rota: string
  const titulo = `VPN fictícia E2E ${Date.now()}`
  const notaInterna = 'Nota reservada fictícia E2E: investigação técnica'
  const publico = 'Mensagem pública fictícia: atendimento iniciado'
  const solucao = 'Solução fictícia: acesso da VPN restabelecido'
  const erros: string[] = []

  async function entrar(page: Page, nome: string, email: string) {
    page.on('pageerror', (erro) => erros.push(erro.message))
    await page.goto('/login')
    await page.getByLabel('Nome', { exact: true }).fill(nome)
    await page.getByLabel('E-mail corporativo').fill(email)
    await page.getByRole('button', { name: 'Entrar', exact: true }).click()
    await expect(page).toHaveURL(/\/(meus-chamados|ti\/fila)$/)
  }

  test.afterAll(async () => {
    await empregado?.close()
    await ti?.close()
  })

  test.afterEach(async ({ browser }, testInfo) => {
    if (browser.isConnected() && testInfo.status !== testInfo.expectedStatus) {
      for (const [nome, page] of [
        ['solicitante', solicitante],
        ['agente', agente],
      ] as const) {
        if (page && !page.isClosed()) {
          const path = testInfo.outputPath(`${nome}-falha.png`)
          await page.screenshot({ path, fullPage: true })
          await testInfo.attach(nome, { path, contentType: 'image/png' })
        }
      }
    }
  })

  test('Funcionário abre, anexa e encontra o chamado na própria lista', async ({
    browser,
    baseURL,
  }) => {
    empregado = await browser.newContext({ baseURL })
    solicitante = await empregado.newPage()
    const resposta = await solicitante.goto('/login')
    expect(resposta?.headers()['content-security-policy']).toContain(
      "script-src 'self'",
    )
    expect(resposta?.headers()['x-content-type-options']).toBe('nosniff')
    expect(resposta?.headers()['referrer-policy']).toBe(
      'strict-origin-when-cross-origin',
    )
    expect(resposta?.headers()['permissions-policy']).toContain('camera=()')
    expect(resposta?.headers()['strict-transport-security']).toBeUndefined()
    await entrar(
      solicitante,
      'Funcionário fictício E2E',
      `funcionario-${Date.now()}@exemplo.invalid`,
    )
    await solicitante
      .getByRole('link', { name: 'Novo chamado', exact: true })
      .click()
    await solicitante.getByLabel('Qual é o problema?').fill(titulo)
    await solicitante
      .getByLabel('Categoria', { exact: true })
      .selectOption({ label: 'Rede e internet' })
    await solicitante
      .getByLabel('Descreva o que precisa')
      .fill('Conexão fictícia indisponível para teste automatizado.')
    await solicitante
      .getByRole('button', { name: 'Abrir chamado', exact: true })
      .click()
    await expect(
      solicitante.getByRole('heading', { name: titulo }),
    ).toBeVisible()
    rota = new URL(solicitante.url()).pathname
    await solicitante.getByLabel('Adicionar arquivo').setInputFiles({
      name: 'diagnostico.csv',
      mimeType: 'text/csv',
      buffer: Buffer.from('tipo,resultado\nVPN,falha\n'),
    })
    await solicitante
      .getByRole('button', { name: 'Enviar anexo', exact: true })
      .click()
    const link = solicitante.getByRole('link', {
      name: 'diagnostico.csv',
      exact: true,
    })
    await expect(link).toBeVisible()
    const arquivo = await solicitante.request.get(
      (await link.getAttribute('href'))!,
    )
    expect(arquivo.status()).toBe(200)
    expect(arquivo.headers()['content-disposition']).toContain('attachment')
    expect(arquivo.headers()['x-content-type-options']).toBe('nosniff')
    expect(await arquivo.text()).toBe('tipo,resultado\nVPN,falha\n')
    const semCsrf = await solicitante.request.post('/api/v1/chamados', {
      data: {},
    })
    expect(semCsrf.status()).toBe(403)
    await solicitante
      .getByRole('link', { name: /Voltar aos meus chamados/ })
      .click()
    await expect(
      solicitante.getByRole('link', { name: titulo, exact: true }),
    ).toBeVisible()
    await solicitante.getByRole('link', { name: titulo, exact: true }).click()
    const detalhe = (await (
      await solicitante.request.get(`/api/v1${rota}`)
    ).json()) as { numero: string }
    type EmailCapturado = {
      Content: { Headers: Record<string, string[]>; Body: string }
    }
    let emails: EmailCapturado[] = []
    await expect
      .poll(
        async () => {
          const caixa = (await (
            await solicitante.request.get(
              'http://localhost:8025/api/v2/messages',
            )
          ).json()) as { items: EmailCapturado[] }
          emails = caixa.items.filter((email) =>
            email.Content.Headers.Subject?.some((assunto) =>
              assunto.includes(`${detalhe.numero} - Novo chamado`),
            ),
          )
          return emails.length
        },
        { timeout: 45_000, intervals: [1000, 2000] },
      )
      .toBe(1)
    expect(emails[0].Content.Headers.To.join(',')).toContain(
      'agente-e2e@exemplo.invalid',
    )
    expect(emails[0].Content.Body).toMatch(/Content-Type: text\/html/i)
    expect(emails[0].Content.Body).toMatch(/Content-Type: text\/plain/i)
  })

  test('TI assume, comenta publicamente, registra nota interna e resolve', async ({
    browser,
    baseURL,
  }) => {
    ti = await browser.newContext({ baseURL })
    agente = await ti.newPage()
    await entrar(agente, 'Agente fictício E2E', 'agente-e2e@exemplo.invalid')
    await agente.goto(rota)
    await agente
      .getByRole('button', { name: 'Assumir chamado', exact: true })
      .click()
    await expect(
      agente.getByRole('button', { name: 'Assumir chamado', exact: true }),
    ).toHaveCount(0)
    await expect(agente.getByLabel('Próximo status')).toBeVisible()
    await agente.getByLabel('Nova mensagem', { exact: true }).fill(publico)
    await agente
      .getByRole('button', { name: 'Enviar mensagem', exact: true })
      .click()
    await expect(agente.getByText(publico, { exact: true })).toBeVisible()
    await agente
      .getByLabel('Nota interna (visível somente à TI)', { exact: true })
      .check()
    await agente
      .getByLabel('Nova nota interna', { exact: true })
      .fill(notaInterna)
    await agente
      .getByRole('button', { name: 'Enviar mensagem', exact: true })
      .click()
    await expect(agente.getByText(notaInterna, { exact: true })).toBeVisible()
    await resolver()
  })

  async function resolver() {
    await agente.getByLabel('Próximo status').selectOption('RESOLVIDO')
    await agente.getByLabel('Solução aplicada').fill(solucao)
    await agente
      .getByRole('button', { name: 'Salvar alterações', exact: true })
      .click()
    await expect(agente.getByText(solucao, { exact: true })).toBeVisible()
  }

  test('Solicitante vê solução pública, reabre e avalia após nova resolução', async ({
    baseURL,
  }, testInfo) => {
    expect(baseURL).toBeTruthy()
    await solicitante.reload()
    await expect(solicitante.getByText(solucao, { exact: true })).toBeVisible()
    await expect(solicitante.getByText(publico, { exact: true })).toBeVisible()
    await expect(
      solicitante.getByText(notaInterna, { exact: true }),
    ).toHaveCount(0)
    await expect(solicitante.getByLabel(/Nota interna/)).toHaveCount(0)
    const timeline = await solicitante.request.get(
      `/api/v1${rota}/linha-do-tempo`,
    )
    expect(timeline.status()).toBe(200)
    expect(await timeline.text()).not.toContain(notaInterna)
    await solicitante.screenshot({
      path: testInfo.outputPath('resolucao-publica.png'),
      fullPage: true,
    })
    await solicitante
      .getByRole('button', { name: 'Reabrir chamado', exact: true })
      .click()
    await expect(
      solicitante.getByText('Aberto', { exact: true }).first(),
    ).toBeVisible()
    await expect(
      solicitante.getByRole('button', { name: 'Reabrir chamado', exact: true }),
    ).toHaveCount(0)
    await agente.reload()
    await agente
      .getByRole('button', { name: 'Assumir chamado', exact: true })
      .click()
    await expect(
      agente.getByRole('button', { name: 'Assumir chamado', exact: true }),
    ).toHaveCount(0)
    await resolver()
    await solicitante.reload()
    await solicitante
      .getByRole('combobox', { name: 'Nota', exact: true })
      .selectOption('4')
    await solicitante
      .getByLabel('Comentário opcional')
      .fill('Avaliação fictícia do atendimento E2E')
    await solicitante
      .getByRole('button', { name: 'Enviar avaliação', exact: true })
      .click()
    await expect(solicitante.getByText('Sua avaliação: 4 de 5')).toBeVisible()
    expect(erros).toEqual([])
  })

  test('Outro funcionário recebe 404 sem título, mensagens ou anexos alheios', async ({
    browser,
    baseURL,
  }) => {
    const contexto = await browser.newContext({ baseURL })
    try {
      const outro = await contexto.newPage()
      await entrar(
        outro,
        'Outro funcionário fictício',
        `outro-${Date.now()}@exemplo.invalid`,
      )
      await outro.goto(rota)
      await expect(
        outro.getByRole('heading', {
          name: 'Chamado não encontrado',
          exact: true,
        }),
      ).toBeVisible()
      await expect(outro.getByText(titulo, { exact: true })).toHaveCount(0)
      await expect(outro.getByText(notaInterna, { exact: true })).toHaveCount(0)
      for (const sufixo of ['', '/linha-do-tempo', '/anexos']) {
        const resposta = await outro.request.get(`/api/v1${rota}${sufixo}`)
        expect(resposta.status()).toBe(404)
        expect(await resposta.text()).not.toContain(titulo)
      }
      expect(erros).toEqual([])
    } finally {
      await contexto.close()
    }
  })
})
