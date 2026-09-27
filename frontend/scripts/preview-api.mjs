import { createServer } from 'node:http'

// Prévia local em memória para navegar pelas telas; a API Java segue como implementação real.
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
const pessoas = [
  {
    id: 1,
    nome: 'Maria Oliveira',
    email: 'maria@exemplo.local',
    perfil: 'FUNCIONARIO',
  },
  {
    id: 2,
    nome: 'Agente de TI',
    email: 'agente@exemplo.local',
    perfil: 'TI_AGENTE',
  },
  {
    id: 3,
    nome: 'Admin de TI',
    email: 'admin@exemplo.local',
    perfil: 'TI_ADMIN',
  },
]
const agora = new Date().toISOString()
const base = {
  abertoPorId: null,
  prioridadeSugerida: null,
  criadoEm: agora,
  atualizadoEm: agora,
  resolvidoEm: null,
  primeiraRespostaEm: null,
  solucao: null,
  prazoResolucao: null,
  version: 0,
}
const chamados = [
  {
    ...base,
    id: 1,
    numero: 'CH-2026-000001',
    titulo: 'Não consigo acessar a VPN',
    descricao:
      'A conexão com a VPN falha ao entrar com minha conta corporativa.',
    solicitanteId: 1,
    categoriaId: 4,
    responsavelId: null,
    prioridade: 'MEDIA',
    status: 'ABERTO',
  },
  {
    ...base,
    id: 2,
    numero: 'CH-2026-000002',
    titulo: 'Notebook reiniciando sozinho',
    descricao: 'O notebook reiniciou duas vezes durante as reuniões de hoje.',
    solicitanteId: 1,
    categoriaId: 2,
    responsavelId: 2,
    prioridade: 'ALTA',
    status: 'EM_ATENDIMENTO',
  },
]
const historico = []
const comentarios = []
const anexos = []
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

function pagina(itens, parametros) {
  const page = Math.max(0, Number(parametros.get('page')) || 0)
  const size = Math.min(100, Math.max(1, Number(parametros.get('size')) || 10))
  return {
    content: itens.slice(page * size, (page + 1) * size),
    page,
    size,
    totalElements: itens.length,
    totalPages: Math.ceil(itens.length / size),
  }
}

function ti() {
  return usuario?.perfil === 'TI_AGENTE' || usuario?.perfil === 'TI_ADMIN'
}

function registrar(chamado, campo, anterior, novo) {
  if (anterior === novo) return
  historico.push({
    id: historico.length + 1,
    chamadoId: chamado.id,
    usuarioId: usuario.id,
    campo,
    valorAnterior: anterior == null ? null : String(anterior),
    valorNovo: novo == null ? null : String(novo),
    criadoEm: new Date().toISOString(),
  })
}

function validarCsrf(request, response) {
  if (request.headers['x-csrf-token'] === csrfToken) return true
  responder(response, 403, { detail: 'CSRF inválido' })
  return false
}

const server = createServer(async (request, response) => {
  const url = new URL(request.url, 'http://' + host + ':' + port)
  const caminho = url.pathname
  const autenticado = usuario && request.headers.cookie?.includes('preview=1')
  try {
    if (request.method === 'GET' && caminho === '/api/v1/auth/config')
      return responder(response, 200, {
        modo: 'dev',
        urlLogin: null,
        previewDemo: true,
      })
    if (request.method === 'GET' && caminho === '/api/v1/auth/csrf')
      return responder(response, 200, { token: csrfToken })
    if (request.method === 'POST' && caminho === '/api/v1/auth/dev/login') {
      if (!validarCsrf(request, response)) return
      const dados = await corpoJson(request)
      if (!dados.nome?.trim() || !dados.email?.includes('@'))
        return responder(response, 400, { detail: 'Nome e e-mail inválidos' })
      const email = dados.email.trim().toLowerCase()
      const conhecido = pessoas.find((pessoa) => pessoa.email === email)
      usuario = conhecido
        ? { ...conhecido }
        : {
            id: pessoas.length + 1,
            nome: dados.nome.trim(),
            email,
            perfil: 'FUNCIONARIO',
          }
      if (!conhecido) pessoas.push(usuario)
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
    if (request.method === 'GET' && caminho === '/api/v1/usuarios/busca') {
      if (!ti())
        return responder(response, 403, { detail: 'Acesso restrito à TI' })
      const texto = url.searchParams.get('texto')?.trim().toLowerCase() ?? ''
      if (texto.length < 2 || texto.length > 100)
        return responder(response, 400, {
          detail: 'Informe entre 2 e 100 caracteres',
        })
      return responder(
        response,
        200,
        pessoas
          .filter(
            (pessoa) =>
              !url.searchParams.has('somenteTi') ||
              url.searchParams.get('somenteTi') !== 'true' ||
              pessoa.perfil !== 'FUNCIONARIO',
          )
          .filter(
            (pessoa) =>
              pessoa.nome.toLowerCase().includes(texto) ||
              pessoa.email.includes(texto),
          )
          .slice(0, 20),
      )
    }
    if (request.method === 'GET' && caminho === '/api/v1/chamados/meus')
      return responder(
        response,
        200,
        pagina(
          [...chamados]
            .filter((item) => item.solicitanteId === usuario.id)
            .reverse(),
          url.searchParams,
        ),
      )
    if (request.method === 'GET' && caminho === '/api/v1/chamados') {
      if (!ti())
        return responder(response, 403, { detail: 'Acesso restrito à TI' })
      let itens = [...chamados]
      for (const campo of [
        'status',
        'prioridade',
        'categoriaId',
        'responsavelId',
      ]) {
        const valor = url.searchParams.get(campo)
        if (valor) itens = itens.filter((item) => String(item[campo]) === valor)
      }
      if (url.searchParams.get('semResponsavel') === 'true')
        itens = itens.filter((item) => !item.responsavelId)
      if (url.searchParams.get('meus') === 'true')
        itens = itens.filter((item) => item.responsavelId === usuario.id)
      const texto = url.searchParams.get('texto')?.trim().toLowerCase()
      if (texto)
        itens = itens.filter((item) =>
          [item.numero, item.titulo, item.descricao].some((campo) =>
            campo.toLowerCase().includes(texto),
          ),
        )
      const desde = url.searchParams.get('desde')
      const ate = url.searchParams.get('ate')
      if (desde)
        itens = itens.filter((item) => item.criadoEm.slice(0, 10) >= desde)
      if (ate) itens = itens.filter((item) => item.criadoEm.slice(0, 10) <= ate)
      return responder(response, 200, pagina(itens.reverse(), url.searchParams))
    }
    const detalhe = /^\/api\/v1\/chamados\/(\d+)$/.exec(caminho)
    const assumir = /^\/api\/v1\/chamados\/(\d+)\/assumir$/.exec(caminho)
    const caminhoHistorico = /^\/api\/v1\/chamados\/(\d+)\/historico$/.exec(
      caminho,
    )
    const caminhoLinhaTempo =
      /^\/api\/v1\/chamados\/(\d+)\/linha-do-tempo$/.exec(caminho)
    if (caminhoLinhaTempo && request.method === 'GET') {
      const chamado = chamados.find(
        (item) => item.id === Number(caminhoLinhaTempo[1]),
      )
      if (!chamado || (!ti() && chamado.solicitanteId !== usuario.id))
        return responder(response, 404, { detail: 'Chamado não encontrado' })
      const eventos = [
        ...comentarios
          .filter((item) => item.chamadoId === chamado.id && !item.interno)
          .map((item) => ({
            id: item.id,
            tipo: 'COMENTARIO',
            autorId: item.autorId,
            texto: item.texto,
            status: null,
            criadoEm: item.criadoEm,
          })),
        ...historico
          .filter(
            (item) => item.chamadoId === chamado.id && item.campo === 'status',
          )
          .map((item) => ({
            id: item.id,
            tipo: 'STATUS',
            autorId: item.usuarioId,
            texto: null,
            status: item.valorNovo,
            criadoEm: item.criadoEm,
          })),
      ].sort(
        (a, b) =>
          a.criadoEm.localeCompare(b.criadoEm) ||
          a.tipo.localeCompare(b.tipo) ||
          a.id - b.id,
      )
      return responder(response, 200, pagina(eventos, url.searchParams))
    }
    const caminhoComentarios = /^\/api\/v1\/chamados\/(\d+)\/comentarios$/.exec(
      caminho,
    )
    const caminhoAnexos = /^\/api\/v1\/chamados\/(\d+)\/anexos$/.exec(caminho)
    const caminhoDownload = /^\/api\/v1\/anexos\/(\d+)\/download$/.exec(caminho)
    if (caminhoComentarios || caminhoAnexos) {
      const match = caminhoComentarios ?? caminhoAnexos
      const chamado = chamados.find((item) => item.id === Number(match[1]))
      if (!chamado || (!ti() && chamado.solicitanteId !== usuario.id))
        return responder(response, 404, { detail: 'Chamado não encontrado' })
      const lista = caminhoComentarios ? comentarios : anexos
      if (request.method === 'GET')
        return responder(
          response,
          200,
          pagina(
            lista.filter(
              (item) =>
                item.chamadoId === chamado.id && (ti() || !item.interno),
            ),
            url.searchParams,
          ),
        )
      if (request.method === 'POST') {
        if (!validarCsrf(request, response)) return
        if (['RESOLVIDO', 'FECHADO'].includes(chamado.status))
          return responder(response, 409, { detail: 'Chamado concluído' })
        if (caminhoComentarios) {
          const dados = await corpoJson(request)
          if (!dados.texto?.trim())
            return responder(response, 400, { detail: 'Informe a mensagem' })
          if (dados.interno && !ti())
            return responder(response, 403, {
              detail: 'Nota interna exclusiva da TI',
            })
          const momento = new Date().toISOString()
          const item = {
            id: comentarios.length + 1,
            chamadoId: chamado.id,
            autorId: usuario.id,
            texto: dados.texto.trim(),
            interno: Boolean(dados.interno),
            criadoEm: momento,
          }
          comentarios.push(item)
          if (ti() && !item.interno && !chamado.primeiraRespostaEm)
            chamado.primeiraRespostaEm = momento
          if (!ti() && chamado.status === 'AGUARDANDO_USUARIO') {
            registrar(chamado, 'status', chamado.status, 'EM_ATENDIMENTO')
            chamado.status = 'EM_ATENDIMENTO'
            chamado.version++
            chamado.atualizadoEm = momento
          }
          return responder(response, 201, item)
        }
        const partes = []
        let tamanho = 0
        for await (const parte of request) {
          partes.push(parte)
          tamanho += parte.length
          if (tamanho > 11 * 1024 * 1024)
            return responder(response, 413, { detail: 'Anexo excede 10 MB' })
        }
        const corpo = Buffer.concat(partes)
        const texto = corpo.toString('latin1')
        const nome = /filename="([^"]+)"/.exec(texto)?.[1]?.split(/[\\/]/).pop()
        const interno = /name="interno"\r\n\r\ntrue/.test(texto)
        if (!nome)
          return responder(response, 400, { detail: 'Selecione um arquivo' })
        if (interno && !ti())
          return responder(response, 403, {
            detail: 'Anexo interno exclusivo da TI',
          })
        const item = {
          id: anexos.length + 1,
          chamadoId: chamado.id,
          comentarioId: null,
          nomeOriginal: nome,
          tipoMime: nome.endsWith('.pdf') ? 'application/pdf' : 'text/plain',
          tamanho,
          criadoPor: usuario.id,
          interno,
          criadoEm: new Date().toISOString(),
        }
        anexos.push(item)
        return responder(response, 201, item)
      }
    }
    if (caminhoDownload && request.method === 'GET') {
      const item = anexos.find(
        (anexo) => anexo.id === Number(caminhoDownload[1]),
      )
      const chamado = chamados.find((ch) => ch.id === item?.chamadoId)
      if (
        !item ||
        !chamado ||
        (!ti() && (chamado.solicitanteId !== usuario.id || item.interno))
      )
        return responder(response, 404, { detail: 'Anexo não encontrado' })
      response.writeHead(200, {
        'Content-Type': item.tipoMime,
        'Content-Disposition':
          'attachment; filename="' + item.nomeOriginal + '"',
        'Cache-Control': 'no-store',
      })
      response.end('Arquivo de demonstração da prévia local.')
      return
    }
    if (request.method === 'GET' && caminhoHistorico) {
      if (!ti())
        return responder(response, 403, { detail: 'Acesso restrito à TI' })
      const chamado = chamados.find(
        (item) => item.id === Number(caminhoHistorico[1]),
      )
      if (!chamado)
        return responder(response, 404, { detail: 'Chamado não encontrado' })
      return responder(
        response,
        200,
        pagina(
          historico.filter((item) => item.chamadoId === chamado.id).reverse(),
          url.searchParams,
        ),
      )
    }
    if (detalhe && request.method === 'GET') {
      const chamado = chamados.find((item) => item.id === Number(detalhe[1]))
      if (!chamado || (!ti() && chamado.solicitanteId !== usuario.id))
        return responder(response, 404, { detail: 'Chamado não encontrado' })
      return responder(response, 200, chamado)
    }
    if (assumir && request.method === 'POST') {
      if (!ti())
        return responder(response, 403, { detail: 'Acesso restrito à TI' })
      if (!validarCsrf(request, response)) return
      const chamado = chamados.find((item) => item.id === Number(assumir[1]))
      if (!chamado)
        return responder(response, 404, { detail: 'Chamado não encontrado' })
      const dados = await corpoJson(request)
      if (
        dados.version !== chamado.version ||
        chamado.responsavelId ||
        ['RESOLVIDO', 'FECHADO'].includes(chamado.status)
      )
        return responder(response, 409, {
          detail: 'Chamado já assumido ou alterado',
        })
      const statusAnterior = chamado.status
      chamado.responsavelId = usuario.id
      if (chamado.status === 'ABERTO') chamado.status = 'EM_ATENDIMENTO'
      chamado.version++
      chamado.atualizadoEm = new Date().toISOString()
      registrar(chamado, 'responsavel', null, usuario.id)
      registrar(chamado, 'status', statusAnterior, chamado.status)
      return responder(response, 200, chamado)
    }
    if (detalhe && request.method === 'PATCH') {
      if (!ti())
        return responder(response, 403, { detail: 'Acesso restrito à TI' })
      if (!validarCsrf(request, response)) return
      const chamado = chamados.find((item) => item.id === Number(detalhe[1]))
      if (!chamado)
        return responder(response, 404, { detail: 'Chamado não encontrado' })
      const dados = await corpoJson(request)
      if (dados.version !== chamado.version)
        return responder(response, 409, {
          detail: 'O chamado foi alterado por outra pessoa',
        })
      if (
        dados.responsavelId &&
        !pessoas.some(
          (pessoa) =>
            pessoa.id === dados.responsavelId &&
            pessoa.perfil !== 'FUNCIONARIO',
        )
      )
        return responder(response, 400, {
          detail: 'Responsável deve pertencer à TI',
        })
      const anterior = { ...chamado }
      if (dados.responsavelId) {
        chamado.responsavelId = dados.responsavelId
        if (chamado.status === 'ABERTO') chamado.status = 'EM_ATENDIMENTO'
      }
      if (dados.removerResponsavel) chamado.responsavelId = null
      if (dados.categoriaId) chamado.categoriaId = dados.categoriaId
      if (dados.prioridade) chamado.prioridade = dados.prioridade
      if (dados.status && dados.status !== chamado.status) {
        const transicoes = {
          EM_ATENDIMENTO: ['AGUARDANDO_USUARIO', 'RESOLVIDO'],
          AGUARDANDO_USUARIO: ['EM_ATENDIMENTO'],
        }
        if (!transicoes[chamado.status]?.includes(dados.status))
          return responder(response, 409, {
            detail: 'Transição de status inválida',
          })
        if (dados.status === 'RESOLVIDO' && !dados.solucao?.trim())
          return responder(response, 400, { detail: 'Informe a solução' })
        chamado.status = dados.status
        if (dados.status === 'RESOLVIDO') {
          chamado.solucao = dados.solucao.trim()
          chamado.resolvidoEm = new Date().toISOString()
        }
      }
      for (const campo of [
        'responsavelId',
        'status',
        'prioridade',
        'categoriaId',
      ]) {
        const historicoCampo =
          { responsavelId: 'responsavel', categoriaId: 'categoria' }[campo] ??
          campo
        registrar(chamado, historicoCampo, anterior[campo], chamado[campo])
      }
      chamado.version++
      chamado.atualizadoEm = new Date().toISOString()
      return responder(response, 200, chamado)
    }
    if (request.method === 'POST' && caminho === '/api/v1/chamados') {
      if (!validarCsrf(request, response)) return
      const dados = await corpoJson(request)
      if (
        !dados.titulo?.trim() ||
        !dados.descricao?.trim() ||
        !categorias.some((categoria) => categoria.id === dados.categoriaId)
      )
        return responder(response, 400, {
          detail: 'Dados do chamado inválidos',
        })
      if (dados.solicitanteId && !ti())
        return responder(response, 400, {
          detail: 'Solicitante é definido pela sessão',
        })
      const id = chamados.length + 1
      const momento = new Date().toISOString()
      const chamado = {
        ...base,
        id,
        numero: 'CH-2026-' + String(id).padStart(6, '0'),
        titulo: dados.titulo.trim(),
        descricao: dados.descricao.trim(),
        solicitanteId: dados.solicitanteId ?? usuario.id,
        abertoPorId: dados.solicitanteId ? usuario.id : null,
        categoriaId: dados.categoriaId,
        responsavelId: null,
        prioridade: 'MEDIA',
        prioridadeSugerida: dados.prioridadeSugerida ?? null,
        status: 'ABERTO',
        criadoEm: momento,
        atualizadoEm: momento,
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
  console.log('API simulada da prévia em http://' + host + ':' + port)
})
