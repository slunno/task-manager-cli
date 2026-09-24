import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'
import {
  assumirChamado,
  getCategorias,
  getFila,
  type FiltrosFila,
  type Prioridade,
  type StatusChamado,
} from '../../api/chamados'
import { Badge } from '../../components/ui/badge'
import { Button } from '../../components/ui/button'
import { Card, CardContent } from '../../components/ui/card'
import { dataHora } from '../../lib/dataHora'
import { AreaPage } from '../auth/AreaPage'
import { prioridadeTexto, statusTexto } from './formatacao'
import { PessoaPicker } from './PessoaPicker'

const STATUS: StatusChamado[] = [
  'ABERTO',
  'EM_ATENDIMENTO',
  'AGUARDANDO_USUARIO',
  'RESOLVIDO',
  'FECHADO',
]
const PRIORIDADES: Prioridade[] = ['BAIXA', 'MEDIA', 'ALTA', 'CRITICA']

function FiltroResponsavel({ inicial }: { inicial: string }) {
  const [id, setId] = useState(inicial)
  return (
    <div>
      <PessoaPicker
        titulo="Responsável"
        somenteTi
        selecionadoId={id}
        onSelect={setId}
      />
      <input type="hidden" name="responsavelId" value={id} />
    </div>
  )
}

function positivo(valor: string | null): number | undefined {
  const numero = Number(valor)
  return valor && Number.isSafeInteger(numero) && numero > 0
    ? numero
    : undefined
}

function pagina(valor: string | null): number {
  const numero = Number(valor)
  return Number.isSafeInteger(numero) && numero >= 0 ? numero : 0
}

function filtrosDaUrl(parametros: URLSearchParams): FiltrosFila {
  const status = parametros.get('status')
  const prioridade = parametros.get('prioridade')
  return {
    page: pagina(parametros.get('page')),
    size: 10,
    sort: 'criadoEm,desc',
    ...(status && STATUS.includes(status as StatusChamado)
      ? { status: status as StatusChamado }
      : {}),
    ...(prioridade && PRIORIDADES.includes(prioridade as Prioridade)
      ? { prioridade: prioridade as Prioridade }
      : {}),
    ...(positivo(parametros.get('responsavelId'))
      ? { responsavelId: positivo(parametros.get('responsavelId')) }
      : {}),
    ...(positivo(parametros.get('categoriaId'))
      ? { categoriaId: positivo(parametros.get('categoriaId')) }
      : {}),
    ...(positivo(parametros.get('setorId'))
      ? { setorId: positivo(parametros.get('setorId')) }
      : {}),
    ...(parametros.get('texto')?.trim()
      ? { texto: parametros.get('texto')?.trim() }
      : {}),
    ...(parametros.get('desde')
      ? { desde: parametros.get('desde') ?? '' }
      : {}),
    ...(parametros.get('ate') ? { ate: parametros.get('ate') ?? '' } : {}),
    semResponsavel: parametros.get('semResponsavel') === 'true',
    meus: parametros.get('meus') === 'true',
  }
}

export function FilaTiPage() {
  const [parametros, setParametros] = useSearchParams()
  const chaveFiltros = parametros.toString()
  const filtros = filtrosDaUrl(parametros)
  const cliente = useQueryClient()
  const lista = useQuery({
    queryKey: ['fila-ti', chaveFiltros],
    queryFn: () => getFila(filtros),
    retry: false,
  })
  const categorias = useQuery({
    queryKey: ['categorias'],
    queryFn: getCategorias,
    staleTime: 60_000,
  })
  const assumir = useMutation({
    mutationFn: ({ id, version }: { id: number; version: number }) =>
      assumirChamado(id, version),
    onSettled: () => void cliente.invalidateQueries({ queryKey: ['fila-ti'] }),
  })

  function aplicar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const dados = new FormData(evento.currentTarget)
    const novos = new URLSearchParams()
    for (const [campo, valor] of dados.entries()) {
      if (typeof valor === 'string' && valor.trim())
        novos.set(campo, valor.trim())
    }
    if (novos.has('responsavelId')) {
      novos.delete('semResponsavel')
      novos.delete('meus')
    } else if (novos.has('semResponsavel')) {
      novos.delete('meus')
    }
    setParametros(novos)
  }

  function atalho(tipo: 'semResponsavel' | 'meus' | 'todos') {
    const novos = new URLSearchParams(parametros)
    novos.delete('page')
    novos.delete('semResponsavel')
    novos.delete('meus')
    novos.delete('responsavelId')
    if (tipo !== 'todos') novos.set(tipo, 'true')
    setParametros(novos)
  }

  function mudarPagina(destino: number) {
    const novos = new URLSearchParams(parametros)
    novos.set('page', String(destino))
    setParametros(novos)
  }

  return (
    <AreaPage
      titulo="Fila da TI"
      descricao="Encontre, assuma e acompanhe os chamados da equipe."
    >
      <div className="mt-8 flex flex-wrap gap-2">
        <Button variant="outline" onClick={() => atalho('todos')}>
          Todos
        </Button>
        <Button variant="outline" onClick={() => atalho('semResponsavel')}>
          Sem responsável
        </Button>
        <Button variant="outline" onClick={() => atalho('meus')}>
          Meus
        </Button>
        <Button
          variant="outline"
          disabled
          title="Disponível após a configuração do SLA"
        >
          Vencendo SLA (em breve)
        </Button>
      </div>

      <form
        key={chaveFiltros}
        onSubmit={aplicar}
        className="mt-6 grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 sm:grid-cols-2 lg:grid-cols-4"
      >
        <label className="text-sm font-medium">
          Buscar título, número ou descrição
          <input
            name="texto"
            maxLength={200}
            defaultValue={parametros.get('texto') ?? ''}
            className="mt-2 h-10 w-full rounded-md border border-slate-300 px-3"
          />
        </label>
        <label className="text-sm font-medium">
          Status
          <select
            name="status"
            defaultValue={parametros.get('status') ?? ''}
            className="mt-2 h-10 w-full rounded-md border border-slate-300 bg-white px-3"
          >
            <option value="">Todos</option>
            {STATUS.map((valor) => (
              <option key={valor} value={valor}>
                {statusTexto[valor]}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm font-medium">
          Prioridade
          <select
            name="prioridade"
            defaultValue={parametros.get('prioridade') ?? ''}
            className="mt-2 h-10 w-full rounded-md border border-slate-300 bg-white px-3"
          >
            <option value="">Todas</option>
            {PRIORIDADES.map((valor) => (
              <option key={valor} value={valor}>
                {prioridadeTexto[valor]}
              </option>
            ))}
          </select>
        </label>
        <label className="text-sm font-medium">
          Categoria
          <select
            name="categoriaId"
            defaultValue={parametros.get('categoriaId') ?? ''}
            className="mt-2 h-10 w-full rounded-md border border-slate-300 bg-white px-3"
          >
            <option value="">Todas</option>
            {categorias.data?.map((categoria) => (
              <option key={categoria.id} value={categoria.id}>
                {categoria.nome}
              </option>
            ))}
          </select>
        </label>
        <details className="sm:col-span-2 lg:col-span-4">
          <summary className="cursor-pointer text-sm font-semibold text-ocean">
            Filtros avançados
          </summary>
          <div className="mt-3 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <FiltroResponsavel
              inicial={parametros.get('responsavelId') ?? ''}
            />
            <label className="text-sm font-medium">
              Setor (ID)
              <input
                name="setorId"
                type="number"
                min={1}
                defaultValue={parametros.get('setorId') ?? ''}
                className="mt-2 h-10 w-full rounded-md border border-slate-300 px-3"
              />
            </label>
            <label className="text-sm font-medium">
              De
              <input
                name="desde"
                type="date"
                defaultValue={parametros.get('desde') ?? ''}
                className="mt-2 h-10 w-full rounded-md border border-slate-300 px-3"
              />
            </label>
            <label className="text-sm font-medium">
              Até
              <input
                name="ate"
                type="date"
                defaultValue={parametros.get('ate') ?? ''}
                className="mt-2 h-10 w-full rounded-md border border-slate-300 px-3"
              />
            </label>
            <label className="flex items-center gap-2 text-sm font-medium">
              <input
                name="semResponsavel"
                value="true"
                type="checkbox"
                defaultChecked={parametros.get('semResponsavel') === 'true'}
              />
              Sem responsável
            </label>
            <label className="flex items-center gap-2 text-sm font-medium">
              <input
                name="meus"
                value="true"
                type="checkbox"
                defaultChecked={parametros.get('meus') === 'true'}
              />
              Somente meus
            </label>
          </div>
        </details>
        <div className="sm:col-span-2 lg:col-span-4 flex flex-wrap gap-3">
          <Button type="submit">Aplicar filtros</Button>
          <Button
            variant="outline"
            type="button"
            onClick={() => setParametros(new URLSearchParams())}
          >
            Limpar
          </Button>
        </div>
      </form>

      {assumir.isError && (
        <p role="alert" className="mt-5 rounded-lg bg-red-50 p-4 text-red-800">
          {assumir.error.message} Atualize a fila antes de tentar novamente.
        </p>
      )}
      {categorias.isError && (
        <p role="alert" className="mt-5 text-sm text-red-800">
          Não foi possível carregar os nomes das categorias.{' '}
          <button
            className="font-semibold underline"
            onClick={() => void categorias.refetch()}
          >
            Tentar novamente
          </button>
        </p>
      )}
      {lista.isPending && (
        <p role="status" className="mt-8">
          Carregando fila…
        </p>
      )}
      {lista.isError && (
        <div
          role="alert"
          className="mt-8 rounded-xl border border-red-200 bg-red-50 p-5 text-red-800"
        >
          Não foi possível carregar a fila.
          <Button
            variant="outline"
            className="ml-3"
            onClick={() => void lista.refetch()}
          >
            Tentar novamente
          </Button>
        </div>
      )}
      {lista.data?.content.length === 0 && (
        <Card className="mt-8">
          <CardContent className="py-10 text-center">
            <h2 className="text-lg font-semibold">Nenhum chamado encontrado</h2>
            <p className="mt-2 text-slate-600">
              Ajuste os filtros ou volte para todos.
            </p>
          </CardContent>
        </Card>
      )}
      {lista.data && lista.data.content.length > 0 && (
        <>
          <div className="mt-6 overflow-x-auto rounded-xl border border-slate-200 bg-white">
            <table className="min-w-[850px] w-full text-left text-sm">
              <thead className="bg-slate-50 text-slate-600">
                <tr>
                  <th scope="col" className="px-4 py-3">
                    Chamado
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Status
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Prioridade
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Categoria
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Responsável
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Abertura
                  </th>
                  <th scope="col" className="px-4 py-3">
                    Ação
                  </th>
                </tr>
              </thead>
              <tbody>
                {lista.data.content.map((chamado) => (
                  <tr key={chamado.id} className="border-t border-slate-100">
                    <td className="px-4 py-4">
                      <span className="block text-xs font-semibold text-ocean">
                        {chamado.numero}
                      </span>
                      <Link
                        to={`/chamados/${chamado.id}`}
                        className="font-semibold hover:underline"
                      >
                        {chamado.titulo}
                      </Link>
                    </td>
                    <td className="px-4 py-4">
                      <Badge variant="secondary">
                        {statusTexto[chamado.status]}
                      </Badge>
                    </td>
                    <td className="px-4 py-4">
                      {prioridadeTexto[chamado.prioridade]}
                    </td>
                    <td className="px-4 py-4">
                      {categorias.data?.find(
                        (categoria) => categoria.id === chamado.categoriaId,
                      )?.nome ?? 'Categoria'}
                    </td>
                    <td className="px-4 py-4">
                      {chamado.responsavelId ?? '—'}
                    </td>
                    <td className="px-4 py-4">{dataHora(chamado.criadoEm)}</td>
                    <td className="px-4 py-4">
                      {!chamado.responsavelId &&
                        !['RESOLVIDO', 'FECHADO'].includes(chamado.status) && (
                          <Button
                            size="sm"
                            disabled={assumir.isPending}
                            onClick={() =>
                              assumir.mutate({
                                id: chamado.id,
                                version: chamado.version,
                              })
                            }
                          >
                            Assumir
                          </Button>
                        )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="mt-5 flex items-center justify-between gap-3 text-sm">
            <Button
              variant="outline"
              disabled={filtros.page === 0}
              onClick={() => mudarPagina((filtros.page ?? 0) - 1)}
            >
              Anterior
            </Button>
            <span>
              Página {(filtros.page ?? 0) + 1} de {lista.data.totalPages}
            </span>
            <Button
              variant="outline"
              disabled={(filtros.page ?? 0) + 1 >= lista.data.totalPages}
              onClick={() => mudarPagina((filtros.page ?? 0) + 1)}
            >
              Próxima
            </Button>
          </div>
        </>
      )}
    </AreaPage>
  )
}
