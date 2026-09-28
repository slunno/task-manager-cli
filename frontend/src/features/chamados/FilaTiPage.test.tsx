import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import {
  agente,
  paginaVazia,
  resposta,
  caminho,
  metodo,
  montar,
} from '../../test/app'
import type { Chamado } from '../../api/chamados'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

it('mostra a fila e assume um chamado com a versão recebida', async () => {
  let chamado: Chamado = {
    id: 41,
    numero: 'CH-2026-000041',
    titulo: 'VPN sem acesso',
    descricao: 'Não consigo acessar a VPN.',
    solicitanteId: 1,
    abertoPorId: null,
    categoriaId: 2,
    responsavelId: null,
    prioridade: 'MEDIA',
    prioridadeSugerida: null,
    status: 'ABERTO',
    criadoEm: '2026-09-24T12:00:00Z',
    atualizadoEm: '2026-09-24T12:00:00Z',
    resolvidoEm: null,
    fechadoEm: null,
    primeiraRespostaEm: null,
    solucao: null,
    prazoPrimeiraResposta: null,
    prazoResolucao: null,
    slaPausadoEm: null,
    version: 0,
  }
  const fetch = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const path = caminho(input)
    if (path === '/api/v1/me') return resposta(agente)
    if (path === '/api/v1/categorias')
      return resposta([{ id: 2, nome: 'Rede e internet' }])
    if (path === '/api/v1/auth/csrf') return resposta({ token: 'csrf-teste' })
    if (path.startsWith('/api/v1/chamados?'))
      return resposta({
        ...paginaVazia,
        content: [chamado],
        totalElements: 1,
        totalPages: 1,
      })
    if (
      path === '/api/v1/chamados/41/assumir' &&
      metodo(input, init) === 'POST'
    ) {
      chamado = {
        ...chamado,
        responsavelId: 1,
        status: 'EM_ATENDIMENTO',
        version: 1,
      }
      return resposta(chamado)
    }
    throw new Error(`Rota inesperada: ${path}`)
  })
  vi.stubGlobal('fetch', fetch)
  montar('/ti/fila')
  fireEvent.click(await screen.findByRole('button', { name: 'Assumir' }))
  await waitFor(() =>
    expect(
      screen.queryByRole('button', { name: 'Assumir' }),
    ).not.toBeInTheDocument(),
  )
  expect(
    screen
      .getAllByText('Em atendimento')
      .some((elemento) => elemento.tagName === 'DIV'),
  ).toBe(true)
  const pedido = fetch.mock.calls.find(
    ([input, init]) =>
      caminho(input) === '/api/v1/chamados/41/assumir' &&
      metodo(input, init) === 'POST',
  )
  const corpo =
    pedido?.[0] instanceof Request
      ? await pedido[0].clone().json()
      : JSON.parse(String(pedido?.[1]?.body))
  expect(corpo).toEqual({ version: 0 })
})

it('aplica filtros e explica fila vazia', async () => {
  const fetch = vi.fn(async (input: RequestInfo | URL) => {
    const path = caminho(input)
    if (path === '/api/v1/me') return resposta(agente)
    if (path.startsWith('/api/v1/chamados?')) return resposta(paginaVazia)
    return resposta([])
  })
  vi.stubGlobal('fetch', fetch)
  montar('/ti/fila')
  expect(
    await screen.findByRole('heading', { name: 'Nenhum chamado encontrado' }),
  ).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Prioridade'), {
    target: { value: 'ALTA' },
  })
  fireEvent.change(
    screen.getByLabelText('Buscar título, número ou descrição'),
    { target: { value: 'VPN' } },
  )
  fireEvent.click(screen.getByRole('button', { name: 'Aplicar filtros' }))
  await waitFor(() =>
    expect(
      fetch.mock.calls.some(
        ([input]) =>
          caminho(input).includes('prioridade=ALTA') &&
          caminho(input).includes('texto=VPN'),
      ),
    ).toBe(true),
  )
})

it('mostra falha da fila e permite tentar novamente', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL) => {
      const path = caminho(input)
      if (path === '/api/v1/me') return resposta(agente)
      if (path.startsWith('/api/v1/chamados?'))
        return resposta({ detail: 'Falha temporária' }, 503)
      return resposta([])
    }),
  )
  montar('/ti/fila')
  expect(
    await screen.findByText('Não foi possível carregar a fila.'),
  ).toBeInTheDocument()
  expect(
    screen.getByRole('button', { name: 'Tentar novamente' }),
  ).toBeInTheDocument()
})
