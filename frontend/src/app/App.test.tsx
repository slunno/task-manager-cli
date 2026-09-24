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

function resposta(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
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
        const path = String(input)
        if (path === '/api/v1/me')
          return resposta({ detail: 'Autenticação necessária' }, 401)
        if (path === '/api/v1/auth/config')
          return resposta({ modo: 'dev', urlLogin: null })
        if (path === '/api/v1/auth/csrf')
          return resposta({ token: 'csrf-teste' })
        if (path === '/api/v1/auth/dev/login' && init?.method === 'POST')
          return resposta(funcionario)
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
      vi.fn(async () => resposta(funcionario)),
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
})
