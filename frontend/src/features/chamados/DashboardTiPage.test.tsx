import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, expect, it, vi } from 'vitest'
import { DashboardTiPage } from './DashboardTiPage'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

function montar() {
  const cliente = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })
  cliente.setQueryData(['me'], {
    id: 1,
    nome: 'Agente',
    email: 'agente@exemplo.local',
    perfil: 'TI_AGENTE',
  })
  cliente.setQueryData(['avisos'], [])
  cliente.setQueryDefaults(['avisos'], { staleTime: Infinity })
  render(
    <QueryClientProvider client={cliente}>
      <MemoryRouter>
        <DashboardTiPage />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

it('mostra SLA, categorias e horas úteis e aplica o período escolhido', async () => {
  const fetch = vi.fn(
    async () =>
      new Response(
        JSON.stringify({
          totalChamados: 3,
          emAberto: 1,
          vencidos: 0,
          vencendo: 1,
          tempoMedioResolucaoHoras: 4,
          porStatus: { RESOLVIDO: 2 },
          porPrioridade: { MEDIA: 3 },
          porCategoria: { Rede: 3 },
          percentualSlaCumprido: 50,
          resolvidos: 2,
          resolvidosSemTempoUtil: 1,
          desde: '2026-10-01',
          ate: '2026-10-07',
        }),
        { headers: { 'Content-Type': 'application/json' } },
      ),
  )
  vi.stubGlobal('fetch', fetch)
  montar()
  expect(await screen.findByText('50.0%')).toBeInTheDocument()
  expect(screen.getByText('Rede')).toBeInTheDocument()
  expect(screen.getByText(/4.0 horas úteis/)).toBeInTheDocument()
  expect(screen.getByText(/1 resoluções sem medição/)).toBeInTheDocument()
  fireEvent.change(screen.getByLabelText('De'), {
    target: { value: '2026-10-01' },
  })
  fireEvent.change(screen.getByLabelText('Até'), {
    target: { value: '2026-10-07' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Aplicar período' }))
  await waitFor(() =>
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/ti/dashboard?desde=2026-10-01&ate=2026-10-07',
      expect.objectContaining({ credentials: 'same-origin' }),
    ),
  )
})

it('explica erro de período sem esconder a possibilidade de tentar novamente', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(
      async () =>
        new Response(
          JSON.stringify({
            detail: 'Informe um período válido de até 366 dias',
          }),
          { status: 400 },
        ),
    ),
  )
  montar()
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Informe um período válido',
  )
  expect(
    screen.getByRole('button', { name: 'Tentar novamente' }),
  ).toBeInTheDocument()
})
