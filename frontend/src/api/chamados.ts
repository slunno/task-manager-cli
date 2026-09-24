import createClient from 'openapi-fetch'
import { ApiError, getCsrfToken } from './auth'
import type { components, paths } from './generated'

export type Chamado = components['schemas']['ChamadoResponse']
export type Categoria = components['schemas']['CategoriaResponse']
export type CriarChamado = components['schemas']['CriarChamadoRequest']
export type PaginaChamados =
  components['schemas']['PaginaResponseChamadoResponse']
export type Prioridade = Chamado['prioridade']
export type StatusChamado = Chamado['status']

const client = createClient<paths>({
  baseUrl: window.location.origin,
  credentials: 'same-origin',
  headers: { Accept: 'application/json' },
  fetch: (request: Request) => globalThis.fetch(request),
})

function exigir<T>(resultado: {
  data?: T
  error?: unknown
  response: Response
}): T {
  if (resultado.data !== undefined) return resultado.data
  const erro = resultado.error
  const detalhe =
    erro && typeof erro === 'object' && 'detail' in erro
      ? String(erro.detail)
      : 'Não foi possível concluir a operação.'
  throw new ApiError(resultado.response.status, detalhe)
}

export async function getCategorias(): Promise<Categoria[]> {
  return exigir(await client.GET('/api/v1/categorias'))
}

export async function getMeusChamados(page: number): Promise<PaginaChamados> {
  return exigir(
    await client.GET('/api/v1/chamados/meus', {
      params: { query: { page, size: 10, sort: 'criadoEm,desc' } },
    }),
  )
}

export async function getChamado(id: number): Promise<Chamado> {
  return exigir(
    await client.GET('/api/v1/chamados/{id}', {
      params: { path: { id } },
    }),
  )
}

export async function criarChamado(dados: CriarChamado): Promise<Chamado> {
  const token = await getCsrfToken()
  return exigir(
    await client.POST('/api/v1/chamados', {
      body: dados,
      headers: { 'X-CSRF-TOKEN': token },
    }),
  )
}
