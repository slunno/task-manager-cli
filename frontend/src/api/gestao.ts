import { getCsrfToken, request } from './auth'

export interface Aviso {
  id: number
  titulo: string
  mensagem: string
  ativo: boolean
  inicioEm: string
  fimEm: string | null
}

export interface Duplicidade {
  chamadoId: number
  principalId: number | null
  duplicados: number[]
}

export interface LinhaRelatorio {
  setorId: number | null
  setor: string
  categoriaId: number
  categoria: string
  total: number
  resolvidos: number
  mediaResolucaoHoras: number | null
}

export interface Relatorio {
  desde: string
  ate: string
  linhas: LinhaRelatorio[]
}

export function avisosVigentes(): Promise<Aviso[]> {
  return request('/api/v1/avisos')
}
export function avisosAdmin(): Promise<Aviso[]> {
  return request('/api/v1/ti/admin/avisos')
}

export async function salvarAviso(
  dados: Omit<Aviso, 'id'>,
  id?: number,
): Promise<Aviso> {
  return request(
    id ? `/api/v1/ti/admin/avisos/${id}` : '/api/v1/ti/admin/avisos',
    {
      method: id ? 'PUT' : 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': await getCsrfToken(),
      },
      body: JSON.stringify(dados),
    },
  )
}

export function obterDuplicidade(id: number): Promise<Duplicidade> {
  return request(`/api/v1/ti/chamados/${id}/duplicidade`)
}
export async function vincularDuplicidade(
  id: number,
  principalId: number | null,
  version: number,
): Promise<Duplicidade> {
  return request(`/api/v1/ti/chamados/${id}/duplicidade`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'X-CSRF-TOKEN': await getCsrfToken(),
    },
    body: JSON.stringify({ principalId, version }),
  })
}

export function consultarRelatorio(
  desde: string,
  ate: string,
  setorId?: number,
  categoriaId?: number,
): Promise<Relatorio> {
  const params = new URLSearchParams({ desde, ate })
  if (setorId) params.set('setorId', String(setorId))
  if (categoriaId) params.set('categoriaId', String(categoriaId))
  return request(`/api/v1/ti/relatorios?${params}`)
}

export function urlCsvRelatorio(
  desde: string,
  ate: string,
  setorId?: number,
  categoriaId?: number,
): string {
  const params = new URLSearchParams({ desde, ate })
  if (setorId) params.set('setorId', String(setorId))
  if (categoriaId) params.set('categoriaId', String(categoriaId))
  return `/api/v1/ti/relatorios/exportacao.csv?${params}`
}
