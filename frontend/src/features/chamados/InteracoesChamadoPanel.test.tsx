import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import {
  funcionario,
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

it('mostra o andamento público e permite resposta do solicitante', async () => {
  const chamado = {
    id: 41,
    numero: 'CH-2026-000041',
    titulo: 'Acesso à VPN',
    descricao: 'A conexão falha.',
    solicitanteId: 1,
    abertoPorId: null,
    categoriaId: 2,
    responsavelId: 2,
    prioridade: 'MEDIA',
    prioridadeSugerida: null,
    status: 'AGUARDANDO_USUARIO',
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
    if (path === '/api/v1/me') return resposta(funcionario)
    if (path === '/api/v1/categorias')
      return resposta([{ id: 2, nome: 'Rede e internet' }])
    if (path === '/api/v1/chamados/41') return resposta(chamado)
    if (path.startsWith('/api/v1/chamados/41/linha-do-tempo?'))
      return resposta({
        ...paginaVazia,
        content: [
          {
            id: 1,
            tipo: 'STATUS',
            autorId: 2,
            texto: null,
            status: 'AGUARDANDO_USUARIO',
            criadoEm: '2026-09-24T12:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
      })
    if (path.startsWith('/api/v1/chamados/41/anexos?'))
      return resposta(paginaVazia)
    if (path === '/api/v1/auth/csrf') return resposta({ token: 'csrf-teste' })
    if (
      path === '/api/v1/chamados/41/comentarios' &&
      metodo(input, init) === 'POST'
    )
      return resposta(
        {
          id: 1,
          chamadoId: 41,
          autorId: 1,
          texto: 'Ainda falha',
          interno: false,
          criadoEm: '2026-09-24T12:01:00Z',
        },
        201,
      )
    throw new Error(`Rota inesperada: ${path}`)
  })
  vi.stubGlobal('fetch', fetch)
  montar('/chamados/41')
  expect(await screen.findByText('Status atualizado')).toBeInTheDocument()
  expect(screen.queryByLabelText(/Nota interna/)).not.toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('Nova mensagem'), {
    target: { value: 'Ainda falha' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Enviar mensagem' }))
  await waitFor(() =>
    expect(
      fetch.mock.calls.some(
        ([input, init]) =>
          caminho(input) === '/api/v1/chamados/41/comentarios' &&
          metodo(input, init) === 'POST',
      ),
    ).toBe(true),
  )
  const pedido = fetch.mock.calls.find(
    ([input, init]) =>
      caminho(input) === '/api/v1/chamados/41/comentarios' &&
      metodo(input, init) === 'POST',
  )
  const corpo =
    pedido?.[0] instanceof Request
      ? await pedido[0].clone().json()
      : JSON.parse(String(pedido?.[1]?.body))
  expect(corpo).toEqual({ texto: 'Ainda falha', interno: false })
})
