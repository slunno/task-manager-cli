import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { AreaPage } from '../auth/AreaPage'

type Indicadores = {
  totalChamados: number
  emAberto: number
  vencidos: number
  vencendo: number
  tempoMedioResolucaoHoras: number | null
  porStatus: Record<string, number>
  porPrioridade: Record<string, number>
}

async function carregarIndicadores(): Promise<Indicadores> {
  const resposta = await fetch('/api/v1/ti/dashboard', {
    credentials: 'same-origin',
  })
  if (!resposta.ok) throw new Error('Não foi possível carregar os indicadores.')
  return (await resposta.json()) as Indicadores
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
  const consulta = useQuery({
    queryKey: ['dashboard-ti'],
    queryFn: carregarIndicadores,
    refetchInterval: 60_000,
  })
  return (
    <AreaPage
      titulo="Dashboard da TI"
      descricao="Visão atual da operação e dos prazos de atendimento."
    >
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
          Não foi possível carregar os indicadores.{' '}
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
          <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            {[
              ['Total', consulta.data.totalChamados],
              ['Em aberto', consulta.data.emAberto],
              ['Vencendo em 1 hora', consulta.data.vencendo],
              ['Vencidos', consulta.data.vencidos],
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
          <div className="mt-5 grid gap-5 md:grid-cols-2">
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
                  : `${consulta.data.tempoMedioResolucaoHoras.toFixed(1)} horas corridas`}
              </p>
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
