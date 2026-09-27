import { getCsrfToken, request } from './auth'

export interface Artigo {
  id: number
  titulo: string
  conteudo: string
  categoriaId: number | null
  publicado: boolean
  atualizadoEm: string
}

export interface RespostaPronta {
  id: number
  titulo: string
  texto: string
  ativo: boolean
}

export interface FiltroSalvo {
  id: number
  nome: string
  parametros: string
}

export interface Pagina<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

async function enviar<T>(
  path: string,
  method: string,
  body?: unknown,
): Promise<T> {
  return request<T>(path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': await getCsrfToken(),
    },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
}

export function buscarArtigos(
  texto: string,
  page: number,
  categoriaId?: number,
): Promise<Pagina<Artigo>> {
  const params = new URLSearchParams({ texto, page: String(page), size: '10' })
  if (categoriaId) params.set('categoriaId', String(categoriaId))
  return request(`/api/v1/artigos?${params}`)
}

export function obterArtigo(id: number): Promise<Artigo> {
  return request(`/api/v1/artigos/${id}`)
}

export function salvarArtigo(
  dados: Omit<Artigo, 'id' | 'atualizadoEm'>,
  id?: number,
): Promise<Artigo> {
  return enviar(
    id ? `/api/v1/ti/artigos/${id}` : '/api/v1/ti/artigos',
    id ? 'PUT' : 'POST',
    dados,
  )
}

export function listarRespostas(): Promise<RespostaPronta[]> {
  return request('/api/v1/ti/respostas-prontas')
}

export function salvarResposta(
  dados: Omit<RespostaPronta, 'id'>,
  id?: number,
): Promise<RespostaPronta> {
  return enviar(
    id ? `/api/v1/ti/respostas-prontas/${id}` : '/api/v1/ti/respostas-prontas',
    id ? 'PUT' : 'POST',
    dados,
  )
}

export function listarFiltros(): Promise<FiltroSalvo[]> {
  return request('/api/v1/ti/filtros-salvos')
}

export function salvarFiltro(
  nome: string,
  parametros: string,
): Promise<FiltroSalvo> {
  return enviar('/api/v1/ti/filtros-salvos', 'POST', { nome, parametros })
}

export function excluirFiltro(id: number): Promise<void> {
  return enviar(`/api/v1/ti/filtros-salvos/${id}`, 'DELETE')
}
