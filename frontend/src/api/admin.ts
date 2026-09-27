import { getCsrfToken, request } from './auth'
import type { Perfil } from './auth'

export type UsuarioAdmin = {
  id: number
  nome: string
  email: string
  perfil: Perfil
  ativo: boolean
  setorId: number | null
}
export type Setor = { id: number; nome: string; ativo: boolean }
export type PaginaUsuarios = {
  content: UsuarioAdmin[]
  totalElements: number
  totalPages: number
  page: number
}
export type CategoriaAdmin = { id: number; nome: string; ativa: boolean }
export type PoliticaSla = {
  prioridade: 'BAIXA' | 'MEDIA' | 'ALTA' | 'CRITICA'
  horasPrimeiraResposta: number
  horasResolucao: number
}
export type Janela = { diaSemana: number; inicio: string; fim: string }
export type Feriado = { id: number; data: string; descricao: string }
export type Calendario = { expediente: Janela[]; feriados: Feriado[] }
export type RelatorioImportacao = {
  importados: number
  rejeitados: number
  erros: { linha: number; motivo: string }[]
}

async function alterar<T>(
  path: string,
  method: string,
  body?: unknown,
): Promise<T> {
  const token = await getCsrfToken()
  return request<T>(path, {
    method,
    headers: { 'X-CSRF-TOKEN': token, 'Content-Type': 'application/json' },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
}

export const adminApi = {
  usuarios: (page: number) =>
    request<PaginaUsuarios>(`/api/v1/ti/admin/usuarios?page=${page}&size=20`),
  salvarUsuario: (usuario: UsuarioAdmin) =>
    alterar<UsuarioAdmin>(
      `/api/v1/ti/admin/usuarios/${usuario.id}`,
      'PATCH',
      usuario,
    ),
  setores: () => request<Setor[]>('/api/v1/ti/setores'),
  todosSetores: () => request<Setor[]>('/api/v1/ti/admin/setores'),
  criarSetor: (nome: string) =>
    alterar<Setor>('/api/v1/ti/admin/setores', 'POST', { nome }),
  salvarSetor: (setor: Setor) =>
    alterar<Setor>(`/api/v1/ti/admin/setores/${setor.id}`, 'PUT', setor),
  importarUsuarios: async (arquivo: File) => {
    const token = await getCsrfToken()
    const corpo = new FormData()
    corpo.append('arquivo', arquivo)
    return request<RelatorioImportacao>(
      '/api/v1/ti/admin/usuarios/importacao',
      {
        method: 'POST',
        headers: { 'X-CSRF-TOKEN': token },
        body: corpo,
      },
    )
  },
  categorias: () => request<CategoriaAdmin[]>('/api/v1/ti/admin/categorias'),
  criarCategoria: (nome: string) =>
    alterar<CategoriaAdmin>('/api/v1/ti/admin/categorias', 'POST', { nome }),
  salvarCategoria: (categoria: CategoriaAdmin) =>
    alterar<CategoriaAdmin>(
      `/api/v1/ti/admin/categorias/${categoria.id}`,
      'PATCH',
      categoria,
    ),
  slas: () => request<PoliticaSla[]>('/api/v1/ti/admin/slas'),
  salvarSla: (politica: PoliticaSla) =>
    alterar<PoliticaSla>(
      `/api/v1/ti/admin/slas/${politica.prioridade}`,
      'PUT',
      politica,
    ),
  calendario: () => request<Calendario>('/api/v1/ti/admin/calendario'),
  salvarExpediente: (janelas: Janela[]) =>
    alterar<Calendario>(
      '/api/v1/ti/admin/calendario/expediente',
      'PUT',
      janelas,
    ),
  adicionarFeriado: (data: string, descricao: string) =>
    alterar<Feriado>('/api/v1/ti/admin/calendario/feriados', 'POST', {
      data,
      descricao,
    }),
  removerFeriado: (id: number) =>
    alterar<void>(`/api/v1/ti/admin/calendario/feriados/${id}`, 'DELETE'),
}
