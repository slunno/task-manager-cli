import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { AreaPage } from '../auth/AreaPage'
import { useState } from 'react'
import { request } from '../../api/auth'
import type { components } from '../../api/generated'

type Indicadores = components['schemas']['DashboardResponse']

function carregarIndicadores(desde: string, ate: string): Promise<Indicadores> {
  return request(`/api/v1/ti/dashboard?${new URLSearchParams({ desde, ate })}`)
}

function dataLocal(data: Date) {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(data)
}

const nomes: Record<string, string> = {
  ABERTO: 'Aberto',
  EM_ATENDIMENTO: 'Em atendimento',
  AGUARDANDO_USUARIO: 'Aguardando usuário',
  RESOLVIDO: 'Resolvido',
  FECHADO: 'Fechado',
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
  CRITICA: 'Crítica',
}

export function DashboardTiPage() {
  const [desde, setDesde] = useState(() =>
    dataLocal(new Date(Date.now() - 29 * 86400000)),
  )
  const [ate, setAte] = useState(() => dataLocal(new Date()))
  const [periodo, setPeriodo] = useState({ desde, ate })
  const consulta = useQuery({
    queryKey: ['dashboard-ti', periodo],
    queryFn: () => carregarIndicadores(periodo.desde, periodo.ate),
    refetchInterval: 60_000,
  })
  return (
    <AreaPage
      titulo="Dashboard da TI"
      descricao="Indicadores dos chamados abertos no período escolhido."
    >
      <form
        className="mt-8 flex flex-wrap items-end gap-4 rounded-xl border bg-white p-5"
        onSubmit={(evento) => {
          evento.preventDefault()
          setPeriodo({ desde, ate })
        }}
      >
        <label className="text-sm font-semibold">
          De
          <input
            required
            type="date"
            value={desde}
            onChange={(e) => setDesde(e.target.value)}
            className="mt-1 block rounded-md border p-2"
          />
        </label>
        <label className="text-sm font-semibold">
          Até
          <input
            required
            type="date"
            value={ate}
            onChange={(e) => setAte(e.target.value)}
            className="mt-1 block rounded-md border p-2"
          />
        </label>
        <button className="rounded-lg bg-ocean px-4 py-2 font-semibold text-white">
          Aplicar período
        </button>
      </form>
      {consulta.isPending && (
        <p role="status" className="mt-8">
          Carregando indicadores…
        </p>
      )}
      {consulta.isError && (
        <div
          role="alert"
          className="mt-8 rounded-xl bg-red-50 p-5 text-red-800"
        >
          {consulta.error.message}{' '}
          <button
            className="font-semibold underline"
            onClick={() => void consulta.refetch()}
          >
            Tentar novamente
          </button>
        </div>
      )}
      {consulta.data && (
        <>
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            {[
              ['Total', consulta.data.totalChamados],
              ['Em aberto', consulta.data.emAberto],
              ['Vencendo em 1 hora', consulta.data.vencendo],
              ['Vencidos', consulta.data.vencidos],
              [
                'SLA cumprido',
                consulta.data.percentualSlaCumprido == null
                  ? 'Sem resolvidos'
                  : `${consulta.data.percentualSlaCumprido.toFixed(1)}%`,
              ],
            ].map(([rotulo, valor]) => (
              <div
                key={rotulo}
                className="rounded-2xl border border-slate-200 bg-white p-6"
              >
                <p className="text-sm text-slate-600">{rotulo}</p>
                <p className="mt-3 text-3xl font-bold text-ocean">{valor}</p>
              </div>
            ))}
          </div>
          <div className="mt-5 grid gap-5 md:grid-cols-3">
            <section className="rounded-2xl border border-slate-200 bg-white p-6">
              <h2 className="font-semibold">Por status</h2>
              <ul className="mt-4 space-y-3">
                {Object.entries(consulta.data.porStatus).map(
                  ([status, total]) => (
                    <li key={status} className="flex justify-between">
                      <span>{nomes[status] ?? status}</span>
                      <strong>{total}</strong>
                    </li>
                  ),
                )}
              </ul>
            </section>
            <section className="rounded-2xl border border-slate-200 bg-white p-6">
              <h2 className="font-semibold">Por prioridade</h2>
              <ul className="mt-4 space-y-3">
                {Object.entries(consulta.data.porPrioridade).map(
                  ([prioridade, total]) => (
                    <li key={prioridade} className="flex justify-between">
                      <span>{nomes[prioridade] ?? prioridade}</span>
                      <strong>{total}</strong>
                    </li>
                  ),
                )}
              </ul>
              <p className="mt-6 text-sm text-slate-600">
                Tempo médio até a resolução:{' '}
                {consulta.data.tempoMedioResolucaoHoras == null
                  ? 'sem dados'
                  : `${consulta.data.tempoMedioResolucaoHoras.toFixed(1)} horas úteis`}
              </p>
              <p className="mt-2 text-sm text-slate-600">
                Média em expediente, incluindo a espera pelo solicitante.
              </p>
              {consulta.data.resolvidosSemTempoUtil > 0 && (
                <p className="mt-2 text-sm text-amber-800">
                  {consulta.data.resolvidosSemTempoUtil} resoluções sem medição
                  de horas úteis não entram na média.
                </p>
              )}
            </section>
            <section className="rounded-2xl border border-slate-200 bg-white p-6">
              <h2 className="font-semibold">Por categoria</h2>
              <ul className="mt-4 space-y-3">
                {Object.entries(consulta.data.porCategoria).map(
                  ([categoria, total]) => (
                    <li key={categoria} className="flex justify-between gap-3">
                      <span>{categoria}</span>
                      <strong>{total}</strong>
                    </li>
                  ),
                )}
              </ul>
            </section>
          </div>
          <Link
            className="mt-6 inline-block font-semibold text-ocean underline"
            to="/ti/fila?slaVencendo=true"
          >
            Ver chamados vencendo
          </Link>
        </>
      )}
    </AreaPage>
  )
}
