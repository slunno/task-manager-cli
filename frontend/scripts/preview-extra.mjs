// Operações adicionais da prévia local. A API Java mantém as regras de negócio reais.
export function createPreviewExtras() {
  const avaliacoes = new Map()
  const politicas = [
    { prioridade: 'BAIXA', horasPrimeiraResposta: 8, horasResolucao: 40 },
    { prioridade: 'MEDIA', horasPrimeiraResposta: 4, horasResolucao: 24 },
    { prioridade: 'ALTA', horasPrimeiraResposta: 2, horasResolucao: 8 },
    { prioridade: 'CRITICA', horasPrimeiraResposta: 1, horasResolucao: 4 },
  ]
  let expediente = [1, 2, 3, 4, 5].map((diaSemana) => ({
    diaSemana,
    inicio: '09:00',
    fim: '18:00',
  }))
  const feriados = []

  return async function responderExtras(ctx) {
    const {
      request,
      response,
      url,
      usuario,
      pessoas,
      categorias,
      chamados,
      responder,
      corpoJson,
      validarCsrf,
      pagina,
      registrar,
    } = ctx
    const caminho = url.pathname
    const ti = usuario.perfil === 'TI_AGENTE' || usuario.perfil === 'TI_ADMIN'
    const protegido = () => {
      if (validarCsrf(request, response)) return true
      return false
    }
    const operacao =
      /^\/api\/v1\/chamados\/(\d+)\/(avaliacao|reabertura)$/.exec(caminho)
    if (operacao) {
      const chamado = chamados.find((item) => item.id === Number(operacao[1]))
      if (!chamado || (!ti && chamado.solicitanteId !== usuario.id)) {
        responder(response, 404, { detail: 'Chamado não encontrado' })
        return true
      }
      if (operacao[2] === 'avaliacao') {
        if (request.method === 'GET') {
          const item = avaliacoes.get(chamado.id)
          responder(
            response,
            item ? 200 : 404,
            item ?? { detail: 'Avaliação não encontrada' },
          )
          return true
        }
        if (request.method === 'POST') {
          if (!protegido()) return true
          if (chamado.solicitanteId !== usuario.id) {
            responder(response, 403, {
              detail: 'Somente o solicitante pode avaliar',
            })
            return true
          }
          if (
            !['RESOLVIDO', 'FECHADO'].includes(chamado.status) ||
            avaliacoes.has(chamado.id)
          ) {
            responder(response, 409, { detail: 'Avaliação indisponível' })
            return true
          }
          const dados = await corpoJson(request)
          if (
            !Number.isInteger(dados.nota) ||
            dados.nota < 1 ||
            dados.nota > 5 ||
            (dados.comentario?.length ?? 0) > 1000
          ) {
            responder(response, 400, { detail: 'Nota ou comentário inválido' })
            return true
          }
          const item = {
            chamadoId: chamado.id,
            nota: dados.nota,
            comentario: dados.comentario?.trim() || null,
            criadoEm: new Date().toISOString(),
          }
          avaliacoes.set(chamado.id, item)
          responder(response, 201, item)
          return true
        }
      }
      if (operacao[2] === 'reabertura' && request.method === 'POST') {
        if (!protegido()) return true
        if (chamado.solicitanteId !== usuario.id) {
          responder(response, 403, {
            detail: 'Somente o solicitante pode reabrir',
          })
          return true
        }
        const dados = await corpoJson(request)
        if (
          dados.version !== chamado.version ||
          chamado.status !== 'RESOLVIDO' ||
          Date.now() > Date.parse(chamado.resolvidoEm) + 7 * 86400000
        ) {
          responder(response, 409, {
            detail: 'Prazo de reabertura encerrado ou chamado alterado',
          })
          return true
        }
        registrar(chamado, 'status', chamado.status, 'ABERTO')
        registrar(chamado, 'responsavel', chamado.responsavelId, null)
        Object.assign(chamado, {
          status: 'ABERTO',
          responsavelId: null,
          resolvidoEm: null,
          fechadoEm: null,
          solucao: null,
          primeiraRespostaEm: null,
          slaPausadoEm: null,
          prazoPrimeiraResposta: new Date(
            Date.now() + 4 * 3600000,
          ).toISOString(),
          prazoResolucao: new Date(Date.now() + 24 * 3600000).toISOString(),
          atualizadoEm: new Date().toISOString(),
          version: chamado.version + 1,
        })
        avaliacoes.delete(chamado.id)
        responder(response, 200, chamado)
        return true
      }
    }

    if (caminho === '/api/v1/ti/dashboard' && request.method === 'GET') {
      if (!ti) {
        responder(response, 403, { detail: 'Acesso restrito à TI' })
        return true
      }
      const contagens = (campo) =>
        Object.fromEntries(
          [...new Set(chamados.map((item) => item[campo]))].map((valor) => [
            valor,
            chamados.filter((item) => item[campo] === valor).length,
          ]),
        )
      const ativos = chamados.filter(
        (item) => !['RESOLVIDO', 'FECHADO'].includes(item.status),
      )
      const resolvidos = chamados.filter((item) => item.resolvidoEm)
      const tempoMedioResolucaoHoras = resolvidos.length
        ? resolvidos.reduce(
            (soma, item) =>
              soma +
              (Date.parse(item.resolvidoEm) - Date.parse(item.criadoEm)) /
                3600000,
            0,
          ) / resolvidos.length
        : null
      const prazo = (item) =>
        item.prazoResolucao && !item.slaPausadoEm
          ? Date.parse(item.prazoResolucao)
          : null
      responder(response, 200, {
        totalChamados: chamados.length,
        emAberto: ativos.length,
        vencidos: ativos.filter(
          (item) => prazo(item) && prazo(item) < Date.now(),
        ).length,
        vencendo: ativos.filter(
          (item) =>
            prazo(item) &&
            prazo(item) >= Date.now() &&
            prazo(item) <= Date.now() + 3600000,
        ).length,
        tempoMedioResolucaoHoras,
        porStatus: contagens('status'),
        porPrioridade: contagens('prioridade'),
      })
      return true
    }

    if (!caminho.startsWith('/api/v1/ti/admin/')) return false
    if (usuario.perfil !== 'TI_ADMIN') {
      responder(response, 403, {
        detail: 'Acesso restrito ao administrador de TI',
      })
      return true
    }
    const raiz = '/api/v1/ti/admin/'
    if (caminho === raiz + 'usuarios' && request.method === 'GET') {
      responder(
        response,
        200,
        pagina(
          [...pessoas].sort((a, b) => a.nome.localeCompare(b.nome)),
          url.searchParams,
        ),
      )
      return true
    }
    const usuarioMatch = /^\/api\/v1\/ti\/admin\/usuarios\/(\d+)$/.exec(caminho)
    if (usuarioMatch && request.method === 'PATCH') {
      if (!protegido()) return true
      const item = pessoas.find(
        (pessoa) => pessoa.id === Number(usuarioMatch[1]),
      )
      if (!item) {
        responder(response, 404, { detail: 'Usuário não encontrado' })
        return true
      }
      const dados = await corpoJson(request)
      if (
        !dados.nome?.trim() ||
        !dados.email?.includes('@') ||
        !['FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN'].includes(dados.perfil)
      ) {
        responder(response, 400, { detail: 'Dados de usuário inválidos' })
        return true
      }
      if (
        item.perfil === 'TI_ADMIN' &&
        item.ativo &&
        (dados.perfil !== 'TI_ADMIN' || !dados.ativo) &&
        pessoas.filter((pessoa) => pessoa.perfil === 'TI_ADMIN' && pessoa.ativo)
          .length <= 1
      ) {
        responder(response, 400, {
          detail: 'É necessário manter um administrador ativo',
        })
        return true
      }
      Object.assign(item, {
        nome: dados.nome.trim(),
        email: dados.email.trim().toLowerCase(),
        perfil: dados.perfil,
        ativo: Boolean(dados.ativo),
      })
      responder(response, 200, item)
      return true
    }
    if (caminho === raiz + 'usuarios/importacao' && request.method === 'POST') {
      if (!protegido()) return true
      const partes = []
      let tamanho = 0
      for await (const parte of request) {
        partes.push(parte)
        tamanho += parte.length
        if (tamanho > 1000000) {
          responder(response, 413, { detail: 'Arquivo muito grande' })
          return true
        }
      }
      const texto = Buffer.concat(partes).toString('utf8')
      const csv = texto.split('\r\n\r\n')[1]?.split('\r\n--')[0] ?? ''
      const linhas = csv.trim().split(/\r?\n/)
      if (linhas.shift()?.trim() !== 'nome;email;perfil;ativo') {
        responder(response, 400, {
          detail: 'Cabeçalho esperado: nome;email;perfil;ativo',
        })
        return true
      }
      const erros = []
      let importados = 0
      linhas.forEach((linha, indice) => {
        const campos = linha.trim().split(';')
        const email = campos[1]?.trim().toLowerCase()
        if (
          campos.length !== 4 ||
          !email?.includes('@') ||
          !['FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN'].includes(
            campos[2]?.trim(),
          ) ||
          !['true', 'false'].includes(campos[3]?.trim())
        ) {
          erros.push({ linha: indice + 2, motivo: 'Dados inválidos' })
          return
        }
        const alvo = pessoas.find((pessoa) => pessoa.email === email)
        const dados = {
          nome: campos[0].trim(),
          email,
          perfil: campos[2].trim(),
          ativo: campos[3].trim() === 'true',
        }
        if (alvo) Object.assign(alvo, dados)
        else pessoas.push({ id: pessoas.length + 1, ...dados })
        importados++
      })
      responder(response, 200, { importados, rejeitados: erros.length, erros })
      return true
    }
    if (caminho === raiz + 'categorias' && request.method === 'GET') {
      responder(response, 200, categorias)
      return true
    }
    if (caminho === raiz + 'categorias' && request.method === 'POST') {
      if (!protegido()) return true
      const dados = await corpoJson(request)
      if (!dados.nome?.trim()) {
        responder(response, 400, { detail: 'Informe o nome da categoria' })
        return true
      }
      const item = {
        id: Math.max(...categorias.map((c) => c.id)) + 1,
        nome: dados.nome.trim(),
        ativa: true,
      }
      categorias.push(item)
      responder(response, 201, item)
      return true
    }
    const categoriaMatch = /^\/api\/v1\/ti\/admin\/categorias\/(\d+)$/.exec(
      caminho,
    )
    if (categoriaMatch && request.method === 'PATCH') {
      if (!protegido()) return true
      const item = categorias.find((c) => c.id === Number(categoriaMatch[1]))
      if (!item) {
        responder(response, 404, { detail: 'Categoria não encontrada' })
        return true
      }
      const dados = await corpoJson(request)
      Object.assign(item, {
        nome: dados.nome?.trim() || item.nome,
        ativa: Boolean(dados.ativa),
      })
      responder(response, 200, item)
      return true
    }
    if (caminho === raiz + 'slas' && request.method === 'GET') {
      responder(response, 200, politicas)
      return true
    }
    const slaMatch =
      /^\/api\/v1\/ti\/admin\/slas\/(BAIXA|MEDIA|ALTA|CRITICA)$/.exec(caminho)
    if (slaMatch && request.method === 'PUT') {
      if (!protegido()) return true
      const dados = await corpoJson(request)
      if (!(
        dados.horasPrimeiraResposta > 0 &&
        dados.horasResolucao >= dados.horasPrimeiraResposta
      )) {
        responder(response, 400, { detail: 'Horas inválidas' })
        return true
      }
      const item = politicas.find((p) => p.prioridade === slaMatch[1])
      Object.assign(item, {
        horasPrimeiraResposta: dados.horasPrimeiraResposta,
        horasResolucao: dados.horasResolucao,
      })
      responder(response, 200, item)
      return true
    }
    if (caminho === raiz + 'calendario' && request.method === 'GET') {
      responder(response, 200, { expediente, feriados })
      return true
    }
    if (
      caminho === raiz + 'calendario/expediente' &&
      request.method === 'PUT'
    ) {
      if (!protegido()) return true
      const dados = await corpoJson(request)
      if (
        !Array.isArray(dados) ||
        !dados.length ||
        dados.some(
          (janela) =>
            !(
              janela.diaSemana >= 1 &&
              janela.diaSemana <= 7 &&
              janela.inicio < janela.fim
            ),
        )
      ) {
        responder(response, 400, { detail: 'Expediente inválido' })
        return true
      }
      expediente = dados
      responder(response, 200, { expediente, feriados })
      return true
    }
    if (caminho === raiz + 'calendario/feriados' && request.method === 'POST') {
      if (!protegido()) return true
      const dados = await corpoJson(request)
      if (!dados.data || !dados.descricao?.trim()) {
        responder(response, 400, { detail: 'Feriado inválido' })
        return true
      }
      const item = {
        id: feriados.length + 1,
        data: dados.data,
        descricao: dados.descricao.trim(),
      }
      feriados.push(item)
      responder(response, 200, item)
      return true
    }
    const feriadoMatch =
      /^\/api\/v1\/ti\/admin\/calendario\/feriados\/(\d+)$/.exec(caminho)
    if (feriadoMatch && request.method === 'DELETE') {
      if (!protegido()) return true
      const indice = feriados.findIndex(
        (item) => item.id === Number(feriadoMatch[1]),
      )
      if (indice < 0) {
        responder(response, 404, { detail: 'Feriado não encontrado' })
        return true
      }
      feriados.splice(indice, 1)
      responder(response, 204, undefined)
      return true
    }
    return false
  }
}
