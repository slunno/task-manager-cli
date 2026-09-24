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

export type FiltrosFila = NonNullable<
  paths['/api/v1/chamados']['get']['parameters']['query']
>
export type AtualizarChamado = components['schemas']['AtualizarChamadoRequest']
export type HistoricoChamado = components['schemas']['HistoricoResponse']
export type PaginaHistorico =
  components['schemas']['PaginaResponseHistoricoResponse']
export type PessoaBusca = components['schemas']['UsuarioBuscaResponse']

export async function buscarPessoas(
  texto: string,
  somenteTi: boolean,
): Promise<PessoaBusca[]> {
  return exigir(
    await client.GET('/api/v1/usuarios/busca', {
      params: { query: { texto, somenteTi } },
    }),
  )
}

export async function getFila(filtros: FiltrosFila): Promise<PaginaChamados> {
  return exigir(
    await client.GET('/api/v1/chamados', { params: { query: filtros } }),
  )
}

export async function assumirChamado(
  id: number,
  version: number,
): Promise<Chamado> {
  const token = await getCsrfToken()
  return exigir(
    await client.POST('/api/v1/chamados/{id}/assumir', {
      params: { path: { id } },
      body: { version },
      headers: { 'X-CSRF-TOKEN': token },
    }),
  )
}

export async function atualizarChamado(
  id: number,
  dados: AtualizarChamado,
): Promise<Chamado> {
  const token = await getCsrfToken()
  return exigir(
    await client.PATCH('/api/v1/chamados/{id}', {
      params: { path: { id } },
      body: dados,
      headers: { 'X-CSRF-TOKEN': token },
    }),
  )
}

export async function getHistorico(
  id: number,
  page = 0,
): Promise<PaginaHistorico> {
  return exigir(
    await client.GET('/api/v1/chamados/{id}/historico', {
      params: { path: { id }, query: { page, size: 20 } },
    }),
  )
}
