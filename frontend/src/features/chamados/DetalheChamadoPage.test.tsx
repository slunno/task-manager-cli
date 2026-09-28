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

it('permite à TI alterar o status e mantém o histórico restrito', async () => {
  let chamado: Chamado = {
    id: 42,
    numero: 'CH-2026-000042',
    titulo: 'Notebook parado',
    descricao: 'Equipamento sem inicializar.',
    solicitanteId: 2,
    abertoPorId: null,
    categoriaId: 2,
    responsavelId: 1,
    prioridade: 'MEDIA',
    prioridadeSugerida: null,
    status: 'EM_ATENDIMENTO',
    criadoEm: '2026-09-24T12:00:00Z',
    atualizadoEm: '2026-09-24T12:00:00Z',
    resolvidoEm: null,
    fechadoEm: null,
    primeiraRespostaEm: null,
    solucao: null,
    prazoPrimeiraResposta: null,
    prazoResolucao: null,
    slaPausadoEm: null,
    version: 1,
  }
  const fetch = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const path = caminho(input)
    if (path === '/api/v1/me') return resposta(agente)
    if (path === '/api/v1/categorias')
      return resposta([{ id: 2, nome: 'Equipamentos' }])
    if (path === '/api/v1/chamados/42') {
      if (metodo(input, init) === 'PATCH') {
        chamado = { ...chamado, status: 'AGUARDANDO_USUARIO', version: 2 }
        return resposta(chamado)
      }
      return resposta(chamado)
    }
    if (path.startsWith('/api/v1/chamados/42/historico?'))
      return resposta({ ...paginaVazia, size: 20 })
    if (path === '/api/v1/auth/csrf') return resposta({ token: 'csrf-teste' })
    throw new Error(`Rota inesperada: ${path}`)
  })
  vi.stubGlobal('fetch', fetch)
  montar('/chamados/42')
  expect(
    await screen.findByRole('heading', { name: 'Operação da TI' }),
  ).toBeInTheDocument()
  expect(
    screen.getByRole('heading', { name: 'Histórico de alterações' }),
  ).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Próximo status'), {
    target: { value: 'AGUARDANDO_USUARIO' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))
  await waitFor(() =>
    expect(screen.getByText('Aguardando usuário')).toBeInTheDocument(),
  )
  const pedido = fetch.mock.calls.find(
    ([input, init]) =>
      caminho(input) === '/api/v1/chamados/42' &&
      metodo(input, init) === 'PATCH',
  )
  const corpo =
    pedido?.[0] instanceof Request
      ? await pedido[0].clone().json()
      : JSON.parse(String(pedido?.[1]?.body))
  expect(corpo).toEqual({ version: 1, status: 'AGUARDANDO_USUARIO' })
})

it('distingue falha temporária de chamado inexistente sem exibir conteúdo', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL) => {
      const path = caminho(input)
      if (path === '/api/v1/me') return resposta(funcionario)
      if (path === '/api/v1/chamados/77')
        return resposta({ detail: 'Indisponível' }, 503)
      return resposta([])
    }),
  )
  montar('/chamados/77')
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Não foi possível carregar este chamado',
  )
  expect(
    screen.queryByRole('heading', { name: 'Chamado não encontrado' }),
  ).not.toBeInTheDocument()
  expect(
    screen.getByRole('button', { name: 'Tentar novamente' }),
  ).toBeInTheDocument()
})
