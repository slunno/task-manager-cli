export type Perfil = 'FUNCIONARIO' | 'TI_AGENTE' | 'TI_ADMIN'

export interface Usuario {
  id: number
  nome: string
  email: string
  perfil: Perfil
}

export interface AuthConfig {
  modo: 'dev' | 'oidc' | 'disabled'
  urlLogin: string | null
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message)
  }
}

export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const response = await fetch(path, {
    ...options,
    credentials: 'same-origin',
    headers: {
      Accept: 'application/json',
      ...options.headers,
    },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new ApiError(
      response.status,
      body?.detail ?? 'Não foi possível concluir a operação.',
    )
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export async function getMe(): Promise<Usuario | null> {
  try {
    return await request<Usuario>('/api/v1/me')
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null
    throw error
  }
}

export function getAuthConfig(): Promise<AuthConfig> {
  return request<AuthConfig>('/api/v1/auth/config')
}

export async function getCsrfToken(): Promise<string> {
  const response = await request<{ token: string }>('/api/v1/auth/csrf')
  return response.token
}

export async function loginDev(dados: {
  email: string
  nome: string
}): Promise<Usuario> {
  const token = await getCsrfToken()
  return request<Usuario>('/api/v1/auth/dev/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-CSRF-TOKEN': token },
    body: JSON.stringify(dados),
  })
}

export async function logout(): Promise<void> {
  const token = await getCsrfToken()
  await request<void>('/api/v1/auth/logout', {
    method: 'POST',
    headers: { 'X-CSRF-TOKEN': token },
  })
}

export function destinoInicial(perfil: Perfil): string {
  return perfil === 'FUNCIONARIO' ? '/meus-chamados' : '/ti/fila'
}
