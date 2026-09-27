import { getCsrfToken, request } from './auth'

export interface Pagina<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Comentario {
  id: number
  chamadoId: number
  autorId: number
  texto: string
  interno: boolean
  criadoEm: string
}

export interface Anexo {
  id: number
  chamadoId: number
  comentarioId: number | null
  nomeOriginal: string
  tipoMime: string
  tamanho: number
  criadoPor: number
  interno: boolean
  criadoEm: string
}

export function getComentarios(chamadoId: number, page: number) {
  return request<Pagina<Comentario>>(
    `/api/v1/chamados/${chamadoId}/comentarios?page=${page}&size=20`,
  )
}

export async function criarComentario(
  chamadoId: number,
  texto: string,
  interno: boolean,
) {
  const token = await getCsrfToken()
  return request<Comentario>(`/api/v1/chamados/${chamadoId}/comentarios`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token },
    body: JSON.stringify({ texto, interno }),
  })
}

export function getAnexos(chamadoId: number, page: number) {
  return request<Pagina<Anexo>>(
    `/api/v1/chamados/${chamadoId}/anexos?page=${page}&size=20`,
  )
}

export async function enviarAnexo(
  chamadoId: number,
  arquivo: File,
  interno: boolean,
) {
  const token = await getCsrfToken()
  const dados = new FormData()
  dados.append('arquivo', arquivo)
  dados.append('interno', String(interno))
  return request<Anexo>(`/api/v1/chamados/${chamadoId}/anexos`, {
    method: 'POST',
    headers: { 'X-CSRF-TOKEN': token },
    body: dados,
  })
}

export interface LinhaTempoItem {
  id: number
  tipo: 'STATUS' | 'COMENTARIO'
  autorId: number | null
  texto: string | null
  status: string | null
  criadoEm: string
}

export function getLinhaTempo(chamadoId: number, page: number) {
  return request<Pagina<LinhaTempoItem>>(
    `/api/v1/chamados/${chamadoId}/linha-do-tempo?page=${page}&size=20`,
  )
}
