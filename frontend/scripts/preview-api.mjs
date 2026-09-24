import { createServer } from 'node:http'

// Prévia local sem persistência. Nunca substitui a API Java em produção.
const host = '127.0.0.1'
const port = 8188
const csrfToken = 'preview-local-token'
const categorias = [
  { id: 1, nome: 'Acessos e contas' },
  { id: 2, nome: 'Equipamentos' },
  { id: 3, nome: 'Sistemas e aplicativos' },
  { id: 4, nome: 'Rede e internet' },
  { id: 5, nome: 'Outros' },
]
const agora = new Date().toISOString()
const chamados = [
  {
    id: 1,
    numero: 'CH-2026-000001',
    titulo: 'Não consigo acessar a VPN',
    descricao:
      'A conexão com a VPN falha ao entrar com minha conta corporativa.',
    solicitanteId: 1,
    abertoPorId: null,
    categoriaId: 4,
    prioridade: 'MEDIA',
    prioridadeSugerida: null,
    status: 'ABERTO',
    criadoEm: agora,
    atualizadoEm: agora,
    version: 0,
  },
  {
    id: 2,
    numero: 'CH-2026-000002',
    titulo: 'Notebook reiniciando sozinho',
    descricao: 'O notebook reiniciou duas vezes durante as reuniões de hoje.',
    solicitanteId: 1,
    abertoPorId: null,
    categoriaId: 2,
    prioridade: 'ALTA',
    prioridadeSugerida: 'ALTA',
    status: 'EM_ATENDIMENTO',
    criadoEm: agora,
    atualizadoEm: agora,
    version: 0,
  },
]
let usuario = null

function responder(response, status, body, headers = {}) {
  response.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Cache-Control': 'no-store',
    ...headers,
  })
  response.end(body === undefined ? undefined : JSON.stringify(body))
}

async function corpoJson(request) {
  let texto = ''
  for await (const parte of request) {
    texto += parte
    if (texto.length > 20_000) throw new Error('Corpo muito grande')
  }
  return JSON.parse(texto || '{}')
}

const server = createServer(async (request, response) => {
  const caminho = new URL(request.url, `http://${host}:${port}`).pathname
  const autenticado = usuario && request.headers.cookie?.includes('preview=1')

  try {
    if (request.method === 'GET' && caminho === '/api/v1/auth/config')
      return responder(response, 200, { modo: 'dev', urlLogin: null })
    if (request.method === 'GET' && caminho === '/api/v1/auth/csrf')
      return responder(response, 200, { token: csrfToken })
    if (request.method === 'POST' && caminho === '/api/v1/auth/dev/login') {
      if (request.headers['x-csrf-token'] !== csrfToken)
        return responder(response, 403, { detail: 'CSRF inválido' })
      const dados = await corpoJson(request)
      if (!dados.nome?.trim() || !dados.email?.includes('@'))
        return responder(response, 400, { detail: 'Nome e e-mail inválidos' })
      usuario = {
        id: 1,
        nome: dados.nome.trim(),
        email: dados.email.trim(),
        perfil: 'FUNCIONARIO',
      }
      return responder(response, 200, usuario, {
        'Set-Cookie': 'preview=1; HttpOnly; SameSite=Lax; Path=/',
      })
    }
    if (request.method === 'POST' && caminho === '/api/v1/auth/logout') {
      usuario = null
      return responder(response, 204, undefined, {
        'Set-Cookie': 'preview=; Max-Age=0; HttpOnly; SameSite=Lax; Path=/',
      })
    }
    if (!autenticado)
      return responder(response, 401, { detail: 'Sessão não iniciada' })
    if (request.method === 'GET' && caminho === '/api/v1/me')
      return responder(response, 200, usuario)
    if (request.method === 'GET' && caminho === '/api/v1/categorias')
      return responder(response, 200, categorias)
    if (request.method === 'GET' && caminho === '/api/v1/chamados/meus') {
      const url = new URL(request.url, `http://${host}:${port}`)
      const page = Math.max(0, Number(url.searchParams.get('page')) || 0)
      const size = Math.max(1, Number(url.searchParams.get('size')) || 10)
      const ordenados = [...chamados].reverse()
      return responder(response, 200, {
        content: ordenados.slice(page * size, (page + 1) * size),
        page,
        size,
        totalElements: ordenados.length,
        totalPages: Math.ceil(ordenados.length / size),
      })
    }
    const detalhe = /^\/api\/v1\/chamados\/(\d+)$/.exec(caminho)
    if (request.method === 'GET' && detalhe) {
      const chamado = chamados.find((item) => item.id === Number(detalhe[1]))
      return responder(
        response,
        chamado ? 200 : 404,
        chamado ?? { detail: 'Chamado não encontrado' },
      )
    }
    if (request.method === 'POST' && caminho === '/api/v1/chamados') {
      if (request.headers['x-csrf-token'] !== csrfToken)
        return responder(response, 403, { detail: 'CSRF inválido' })
      const dados = await corpoJson(request)
      if (
        !dados.titulo?.trim() ||
        !dados.descricao?.trim() ||
        !categorias.some((categoria) => categoria.id === dados.categoriaId)
      )
        return responder(response, 400, {
          detail: 'Dados do chamado inválidos',
        })
      const id = chamados.length + 1
      const momento = new Date().toISOString()
      const chamado = {
        id,
        numero: `CH-2026-${String(id).padStart(6, '0')}`,
        titulo: dados.titulo.trim(),
        descricao: dados.descricao.trim(),
        solicitanteId: 1,
        abertoPorId: null,
        categoriaId: dados.categoriaId,
        prioridade: dados.prioridadeSugerida ?? 'MEDIA',
        prioridadeSugerida: dados.prioridadeSugerida ?? null,
        status: 'ABERTO',
        criadoEm: momento,
        atualizadoEm: momento,
        version: 0,
      }
      chamados.push(chamado)
      return responder(response, 201, chamado)
    }
    return responder(response, 404, { detail: 'Rota não encontrada na prévia' })
  } catch {
    return responder(response, 400, { detail: 'Requisição inválida' })
  }
})

server.listen(port, host, () => {
  console.log(`API simulada da prévia em http://${host}:${port}`)
})
