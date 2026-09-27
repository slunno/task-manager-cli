import { ApiError, getCsrfToken, request } from './auth'
import type { Chamado } from './chamados'

export type Avaliacao = {
  chamadoId: number
  nota: number
  comentario: string | null
  criadoEm: string
}

export function getAvaliacao(chamadoId: number): Promise<Avaliacao> {
  return request<Avaliacao>(`/api/v1/chamados/${chamadoId}/avaliacao`)
}

export async function avaliar(
  chamadoId: number,
  nota: number,
  comentario: string,
): Promise<Avaliacao> {
  const token = await getCsrfToken()
  return request<Avaliacao>(`/api/v1/chamados/${chamadoId}/avaliacao`, {
    method: 'POST',
    headers: { 'X-CSRF-TOKEN': token, 'Content-Type': 'application/json' },
    body: JSON.stringify({ nota, comentario: comentario.trim() || null }),
  })
}

export async function reabrir(
  chamadoId: number,
  version: number,
): Promise<Chamado> {
  const token = await getCsrfToken()
  return request<Chamado>(`/api/v1/chamados/${chamadoId}/reabertura`, {
    method: 'POST',
    headers: { 'X-CSRF-TOKEN': token, 'Content-Type': 'application/json' },
    body: JSON.stringify({ version }),
  })
}

export function avaliacaoAusente(erro: Error | null): boolean {
  return erro instanceof ApiError && erro.status === 404
}
