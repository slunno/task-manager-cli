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
import type { Chamado } from '../../api/chamados'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
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
  const fetch = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const path = caminho(input)
    if (path === '/api/v1/me') return resposta(funcionario)
    if (path === '/api/v1/categorias')
      return resposta([{ id: 2, nome: 'Acessos e contas' }])
    if (path === '/api/v1/auth/csrf') return resposta({ token: 'csrf-teste' })
    if (path === '/api/v1/chamados' && metodo(input, init) === 'POST')
      return resposta(chamado, 201)
    if (path === '/api/v1/chamados/41') return resposta(chamado)
    throw new Error(`Rota inesperada: ${path}`)
  })
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

it('permite abrir chamado em nome de pessoa escolhida pela busca', async () => {
  const chamado: Chamado = {
    id: 50,
    numero: 'CH-2026-000050',
    titulo: 'VPN indisponível',
    descricao: 'A conexão da VPN não funciona.',
    solicitanteId: 1,
    abertoPorId: 2,
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
    if (path === '/api/v1/me') return resposta({ ...agente, id: 2 })
    if (path === '/api/v1/categorias')
      return resposta([{ id: 2, nome: 'Rede e internet' }])
    if (path.startsWith('/api/v1/usuarios/busca?'))
      return resposta([
        {
          id: 1,
          nome: 'Maria Oliveira',
          email: 'maria@exemplo.local',
          perfil: 'FUNCIONARIO',
        },
      ])
    if (path === '/api/v1/auth/csrf') return resposta({ token: 'csrf-teste' })
    if (path === '/api/v1/chamados' && metodo(input, init) === 'POST')
      return resposta(chamado, 201)
    if (path === '/api/v1/chamados/50') return resposta(chamado)
    if (path.startsWith('/api/v1/chamados/50/historico?'))
      return resposta({ ...paginaVazia, size: 20 })
    throw new Error(`Rota inesperada: ${path}`)
  })
  vi.stubGlobal('fetch', fetch)
  montar('/chamados/novo')
  fireEvent.change(
    await screen.findByLabelText('Abrir em nome de (opcional)'),
    {
      target: { value: 'Maria' },
    },
  )
  fireEvent.click(await screen.findByRole('button', { name: /Maria Oliveira/ }))
  fireEvent.change(screen.getByLabelText('Qual é o problema?'), {
    target: { value: 'VPN indisponível' },
  })
  fireEvent.change(screen.getByLabelText('Categoria'), {
    target: { value: '2' },
  })
  fireEvent.change(screen.getByLabelText('Descreva o que precisa'), {
    target: { value: 'A conexão da VPN não funciona.' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Abrir chamado' }))
  await waitFor(() =>
    expect(
      fetch.mock.calls.some(
        ([input, init]) =>
          caminho(input) === '/api/v1/chamados' &&
          metodo(input, init) === 'POST',
      ),
    ).toBe(true),
  )
  const pedido = fetch.mock.calls.find(
    ([input, init]) =>
      caminho(input) === '/api/v1/chamados' && metodo(input, init) === 'POST',
  )
  const corpo =
    pedido?.[0] instanceof Request
      ? await pedido[0].clone().json()
      : JSON.parse(String(pedido?.[1]?.body))
  expect(corpo.solicitanteId).toBe(1)
})
