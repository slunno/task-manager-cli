import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import {
  funcionario,
  agente,
  paginaVazia,
  resposta,
  caminho,
  metodo,
  montar,
} from '../../test/app'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

it('entra com senha pelo Supabase e preserva o token CSRF', async () => {
  const fetch = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const path = caminho(input)
    if (path === '/api/v1/me')
      return resposta({ detail: 'Autenticação necessária' }, 401)
    if (path === '/api/v1/auth/config')
      return resposta({ modo: 'supabase', urlLogin: null })
    if (path === '/api/v1/auth/csrf') return resposta({ token: 'csrf-teste' })
    if (path === '/api/v1/auth/password/login' && init?.method === 'POST')
      return resposta(funcionario)
    if (path.startsWith('/api/v1/chamados/meus?')) return resposta(paginaVazia)
    if (path === '/api/v1/categorias') return resposta([])
    throw new Error(`Rota inesperada: ${path}`)
  })
  vi.stubGlobal('fetch', fetch)
  montar('/login')
  fireEvent.change(await screen.findByLabelText('E-mail'), {
    target: { value: 'maria@empresa.com' },
  })
  fireEvent.change(screen.getByLabelText('Senha'), {
    target: { value: 'senha-de-teste' },
  })
  expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
  fireEvent.click(screen.getByRole('button', { name: 'Entrar' }))
  expect(
    await screen.findByRole('heading', { name: 'Meus chamados' }),
  ).toBeInTheDocument()
  expect(fetch).toHaveBeenCalledWith(
    '/api/v1/auth/password/login',
    expect.objectContaining({
      credentials: 'same-origin',
      headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'csrf-teste' }),
      body: JSON.stringify({
        email: 'maria@empresa.com',
        senha: 'senha-de-teste',
      }),
    }),
  )
})

it('envia quem não tem sessão para o login e entra em modo dev', async () => {
  const fetch = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const path = caminho(input)
    if (path === '/api/v1/me')
      return resposta({ detail: 'Autenticação necessária' }, 401)
    if (path === '/api/v1/auth/config')
      return resposta({ modo: 'dev', urlLogin: null })
    if (path === '/api/v1/auth/csrf') return resposta({ token: 'csrf-teste' })
    if (path === '/api/v1/auth/dev/login' && init?.method === 'POST')
      return resposta(funcionario)
    if (path.startsWith('/api/v1/chamados/meus?')) return resposta(paginaVazia)
    if (path === '/api/v1/categorias') return resposta([])
    throw new Error(`Rota inesperada: ${path}`)
  })
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
  expect(
    screen.queryByRole('button', {
      name: 'Acessar prévia como agente de TI',
    }),
  ).not.toBeInTheDocument()
  expect(fetch).toHaveBeenCalledWith(
    '/api/v1/auth/dev/login',
    expect.objectContaining({
      method: 'POST',
      credentials: 'same-origin',
      headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'csrf-teste' }),
    }),
  )
})

it('permite sair da visão de funcionário na prévia e abrir a fila da TI', async () => {
  let usuario = funcionario
  const fetch = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const path = caminho(input)
    if (path === '/api/v1/me') return resposta(usuario)
    if (path === '/api/v1/auth/config')
      return resposta({ modo: 'dev', urlLogin: null, previewDemo: true })
    if (path === '/api/v1/auth/csrf') return resposta({ token: 'csrf-teste' })
    if (path === '/api/v1/auth/dev/login' && metodo(input, init) === 'POST') {
      usuario = { ...agente, id: 2 }
      return resposta(usuario)
    }
    if (path.startsWith('/api/v1/chamados/meus?')) return resposta(paginaVazia)
    if (path.startsWith('/api/v1/chamados?')) return resposta(paginaVazia)
    if (path === '/api/v1/categorias') return resposta([])
    throw new Error(`Rota inesperada: ${path}`)
  })
  vi.stubGlobal('fetch', fetch)
  montar('/meus-chamados')
  fireEvent.click(
    await screen.findByRole('button', {
      name: 'Acessar prévia como agente de TI',
    }),
  )
  expect(
    await screen.findByRole('heading', { name: 'Fila da TI' }),
  ).toBeInTheDocument()
  const pedido = fetch.mock.calls.find(
    ([input, init]) =>
      caminho(input) === '/api/v1/auth/dev/login' &&
      metodo(input, init) === 'POST',
  )
  const corpo =
    pedido?.[0] instanceof Request
      ? await pedido[0].clone().json()
      : JSON.parse(String(pedido?.[1]?.body))
  expect(corpo).toEqual({
    nome: 'Agente de TI',
    email: 'agente@exemplo.local',
  })
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
    vi.fn(async (input: RequestInfo | URL) => {
      const path = caminho(input)
      if (path === '/api/v1/me') return resposta(agente)
      if (path.startsWith('/api/v1/chamados?')) return resposta(paginaVazia)
      if (path === '/api/v1/categorias') return resposta([])
      throw new Error(`Rota inesperada: ${path}`)
    }),
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
