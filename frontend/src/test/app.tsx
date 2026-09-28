import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { App } from '../app/App'
export const funcionario = {
  id: 1,
  nome: 'Maria',
  email: 'maria@empresa.com',
  perfil: 'FUNCIONARIO',
}
export const agente = { ...funcionario, perfil: 'TI_AGENTE' }
export const paginaVazia = {
  content: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
}

export function resposta(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

export function caminho(input: RequestInfo | URL): string {
  const endereco = input instanceof Request ? input.url : String(input)
  const url = new URL(endereco, 'http://localhost')
  return url.pathname + url.search
}

export function metodo(
  input: RequestInfo | URL,
  init?: RequestInit,
): string | undefined {
  return input instanceof Request ? input.method : init?.method
}

export function montar(rota: string) {
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
