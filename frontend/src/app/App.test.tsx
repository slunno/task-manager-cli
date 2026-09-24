import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter } from 'react-router'
import { App } from './App'

const funcionario = {
  id: 1,
  nome: 'Maria',
  email: 'maria@empresa.com',
  perfil: 'FUNCIONARIO',
}
const agente = { ...funcionario, perfil: 'TI_AGENTE' }
const paginaVazia = {
  content: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
}

function resposta(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function caminho(input: RequestInfo | URL): string {
  const endereco = input instanceof Request ? input.url : String(input)
  const url = new URL(endereco, 'http://localhost')
  return url.pathname + url.search
}

function metodo(
  input: RequestInfo | URL,
  init?: RequestInit,
): string | undefined {
  return input instanceof Request ? input.method : init?.method
}

function montar(rota: string) {
  const cliente = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  return render(
    <QueryClientProvider client={cliente}>
      <MemoryRouter initialEntries={[rota]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('identidade e acesso', () => {
  it('envia quem não tem sessão para o login e entra em modo dev', async () => {
    const fetch = vi.fn(
      async (input: RequestInfo | URL, init?: RequestInit) => {
        const path = caminho(input)
        if (path === '/api/v1/me')
          return resposta({ detail: 'Autenticação necessária' }, 401)
        if (path === '/api/v1/auth/config')
          return resposta({ modo: 'dev', urlLogin: null })
        if (path === '/api/v1/auth/csrf')
          return resposta({ token: 'csrf-teste' })
        if (path === '/api/v1/auth/dev/login' && init?.method === 'POST')
          return resposta(funcionario)
        if (path.startsWith('/api/v1/chamados/meus?'))
          return resposta(paginaVazia)
        if (path === '/api/v1/categorias') return resposta([])
        throw new Error(`Rota inesperada: ${path}`)
      },
    )
    vi.stubGlobal('fetch', fetch)
    montar('/meus-chamados')
    fireEvent.change(await screen.findByLabelText('Nome'), {
      target: { value: 'Maria' },
    })
    fireEvent.change(screen.getByLabelText('E-mail corporativo'), {
      target: { value: 'maria@empresa.com' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))
    expect(
      await screen.findByRole('heading', { name: 'Meus chamados' }),
    ).toBeInTheDocument()
    expect(
      await screen.findByRole('heading', { name: 'Nenhum chamado por aqui' }),
    ).toBeInTheDocument()
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/auth/dev/login',
      expect.objectContaining({
        method: 'POST',
        credentials: 'same-origin',
        headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'csrf-teste' }),
      }),
    )
  })

  it('redireciona funcionário para sua área e bloqueia rota de TI', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        if (caminho(input).startsWith('/api/v1/chamados/meus?'))
          return resposta(paginaVazia)
        if (caminho(input) === '/api/v1/categorias') return resposta([])
        return resposta(funcionario)
      }),
    )
    montar('/ti/fila')
    expect(
      await screen.findByRole('heading', { name: 'Meus chamados' }),
    ).toBeInTheDocument()
    expect(
      screen.queryByRole('link', { name: 'Administração' }),
    ).not.toBeInTheDocument()
  })

  it('permite agente na fila e barra a administração', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => resposta(agente)),
    )
    montar('/ti/admin/usuarios')
    expect(
      await screen.findByRole('heading', { name: 'Fila da TI' }),
    ).toBeInTheDocument()
    await waitFor(() =>
      expect(
        screen.queryByRole('link', { name: 'Administração' }),
      ).not.toBeInTheDocument(),
    )
  })

  it('abre um chamado e mostra o detalhe sem enviar solicitanteId', async () => {
    const chamado = {
      id: 41,
      numero: 'CH-2026-000041',
      titulo: 'Acesso à VPN',
      descricao: 'Não consigo entrar na VPN desde cedo.',
      solicitanteId: 1,
      abertoPorId: null,
      categoriaId: 2,
      prioridade: 'MEDIA',
      prioridadeSugerida: null,
      status: 'ABERTO',
      criadoEm: '2026-09-24T12:00:00Z',
      atualizadoEm: '2026-09-24T12:00:00Z',
      version: 0,
    }
    const fetch = vi.fn(
      async (input: RequestInfo | URL, init?: RequestInit) => {
        const path = caminho(input)
        if (path === '/api/v1/me') return resposta(funcionario)
        if (path === '/api/v1/categorias')
          return resposta([{ id: 2, nome: 'Acessos e contas' }])
        if (path === '/api/v1/auth/csrf')
          return resposta({ token: 'csrf-teste' })
        if (path === '/api/v1/chamados' && metodo(input, init) === 'POST')
          return resposta(chamado, 201)
        if (path === '/api/v1/chamados/41') return resposta(chamado)
        throw new Error(`Rota inesperada: ${path}`)
      },
    )
    vi.stubGlobal('fetch', fetch)
    montar('/chamados/novo')
    fireEvent.change(await screen.findByLabelText('Qual é o problema?'), {
      target: { value: 'Acesso à VPN' },
    })
    fireEvent.change(await screen.findByLabelText('Categoria'), {
      target: { value: '2' },
    })
    fireEvent.change(screen.getByLabelText('Descreva o que precisa'), {
      target: { value: 'Não consigo entrar na VPN desde cedo.' },
    })
    fireEvent.click(screen.getByRole('button', { name: 'Abrir chamado' }))
    expect(
      await screen.findByRole('heading', { name: 'Acesso à VPN' }),
    ).toBeInTheDocument()
    const pedido = fetch.mock.calls.find(
      ([input, init]) =>
        caminho(input) === '/api/v1/chamados' && metodo(input, init) === 'POST',
    )
    expect(pedido).toBeDefined()
    const corpo =
      pedido?.[0] instanceof Request
        ? await pedido[0].clone().json()
        : JSON.parse(String(pedido?.[1]?.body))
    expect(corpo).not.toHaveProperty('solicitanteId')
  })

  it('apresenta 404 sem expor conteúdo de chamado alheio', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const path = caminho(input)
        if (path === '/api/v1/me') return resposta(funcionario)
        if (path === '/api/v1/categorias') return resposta([])
        if (path === '/api/v1/chamados/99')
          return resposta({ detail: 'Chamado não encontrado' }, 404)
        throw new Error(`Rota inesperada: ${path}`)
      }),
    )
    montar('/chamados/99')
    expect(
      await screen.findByRole('heading', { name: 'Chamado não encontrado' }),
    ).toBeInTheDocument()
    expect(screen.queryByText('Descrição privada')).not.toBeInTheDocument()
  })
})
