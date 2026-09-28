import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { agente, caminho, montar, resposta } from '../../test/app'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

it('aplica período/setor/categoria e exporta CSV com os mesmos filtros', async () => {
  const fetch = vi.fn(async (input: RequestInfo | URL) => {
    const path = caminho(input)
    if (path === '/api/v1/me') return resposta(agente)
    if (path === '/api/v1/avisos') return resposta([])
    if (path === '/api/v1/ti/setores')
      return resposta([{ id: 7, nome: 'Financeiro', ativo: true }])
    if (path === '/api/v1/categorias')
      return resposta([{ id: 2, nome: 'Rede' }])
    if (path.startsWith('/api/v1/ti/relatorios?'))
      return resposta({
        desde: '2026-09-01',
        ate: '2026-09-28',
        linhas: [
          {
            setorId: 7,
            setor: 'Financeiro',
            categoriaId: 2,
            categoria: 'Rede',
            total: 12,
            resolvidos: 10,
            mediaResolucaoHoras: 2.5,
          },
        ],
      })
    throw new Error(`Rota inesperada: ${path}`)
  })
  vi.stubGlobal('fetch', fetch)
  montar('/ti/relatorios')
  expect(await screen.findByRole('table')).toHaveTextContent('2.5 h')
  fireEvent.change(screen.getByLabelText('De'), {
    target: { value: '2026-09-01' },
  })
  fireEvent.change(screen.getByLabelText('Até'), {
    target: { value: '2026-09-28' },
  })
  fireEvent.change(screen.getByLabelText('Setor'), { target: { value: '7' } })
  fireEvent.change(screen.getByLabelText('Categoria'), {
    target: { value: '2' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Aplicar' }))
  await waitFor(() =>
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/ti/relatorios?desde=2026-09-01&ate=2026-09-28&setorId=7&categoriaId=2',
      expect.any(Object),
    ),
  )
  expect(
    await screen.findByRole('link', { name: 'Exportar CSV' }),
  ).toHaveAttribute(
    'href',
    '/api/v1/ti/relatorios/exportacao.csv?desde=2026-09-01&ate=2026-09-28&setorId=7&categoriaId=2',
  )
})

it('permite corrigir período inválido após resposta 400', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL) => {
      const path = caminho(input)
      if (path === '/api/v1/me') return resposta(agente)
      if (path.startsWith('/api/v1/ti/relatorios?'))
        return resposta({ detail: 'Período inválido' }, 400)
      return resposta([])
    }),
  )
  montar('/ti/relatorios')
  expect(await screen.findByRole('alert')).toHaveTextContent('Período inválido')
  expect(
    screen.getByRole('button', { name: 'Tentar novamente' }),
  ).toBeInTheDocument()
  expect(
    screen.queryByRole('link', { name: 'Exportar CSV' }),
  ).not.toBeInTheDocument()
})
