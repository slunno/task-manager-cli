import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router'
import { ApiError } from '../../api/auth'
import { getCategorias, getChamado } from '../../api/chamados'
import { Badge } from '../../components/ui/badge'
import { Card, CardContent, CardHeader } from '../../components/ui/card'
import { dataHora } from '../../lib/dataHora'
import { AreaPage } from '../auth/AreaPage'
import { useMe } from '../auth/useAuth'
import { prioridadeTexto, statusTexto } from './formatacao'
import { GestaoChamado } from './GestaoChamado'
import { HistoricoChamadoPanel } from './HistoricoChamadoPanel'

export function DetalheChamadoPage() {
  const sessao = useMe()
  const ehTi =
    sessao.data?.perfil === 'TI_AGENTE' || sessao.data?.perfil === 'TI_ADMIN'
  const { id } = useParams()
  const chamadoId = Number(id)
  const idValido = Number.isSafeInteger(chamadoId) && chamadoId > 0
  const chamado = useQuery({
    queryKey: ['chamado', chamadoId],
    queryFn: () => getChamado(chamadoId),
    enabled: idValido,
    retry: false,
  })
  const categorias = useQuery({
    queryKey: ['categorias'],
    queryFn: getCategorias,
    staleTime: 60_000,
    retry: false,
  })

  return (
    <AreaPage
      titulo="Detalhe do chamado"
      descricao="Veja as informações e acompanhe o atendimento."
    >
      <Link
        to={ehTi ? '/ti/fila' : '/meus-chamados'}
        className="mt-8 inline-block text-sm font-semibold text-ocean hover:underline"
      >
        ← Voltar {ehTi ? 'à fila da TI' : 'aos meus chamados'}
      </Link>
      {idValido && chamado.isPending && (
        <p role="status" className="mt-8">
          Carregando chamado…
        </p>
      )}
      {(!idValido ||
        (chamado.error instanceof ApiError &&
          chamado.error.status === 404)) && (
        <Card className="mt-6">
          <CardContent className="py-10">
            <h2 className="text-xl font-semibold">Chamado não encontrado</h2>
            <p className="mt-2 text-slate-600">
              Confira o endereço ou volte à sua lista.
            </p>
          </CardContent>
        </Card>
      )}
      {idValido &&
        chamado.isError &&
        !(
          chamado.error instanceof ApiError && chamado.error.status === 404
        ) && (
          <div
            role="alert"
            className="mt-6 rounded-xl bg-red-50 p-5 text-red-800"
          >
            Não foi possível carregar este chamado.{' '}
            <button
              className="font-semibold underline"
              onClick={() => void chamado.refetch()}
            >
              Tentar novamente
            </button>
          </div>
        )}
      {chamado.data && (
        <div className="mt-6 grid gap-5 lg:grid-cols-[minmax(0,2fr)_minmax(250px,1fr)]">
          <Card>
            <CardHeader>
              <p className="text-sm font-semibold text-ocean">
                {chamado.data.numero}
              </p>
              <h2 className="text-2xl font-semibold leading-snug">
                {chamado.data.titulo}
              </h2>
            </CardHeader>
            <CardContent>
              <h3 className="text-sm font-semibold text-slate-600">
                Descrição
              </h3>
              <p className="mt-3 whitespace-pre-wrap leading-7">
                {chamado.data.descricao}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="space-y-5 p-6">
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Status
                </p>
                <Badge className="mt-2" variant="secondary">
                  {statusTexto[chamado.data.status]}
                </Badge>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Categoria
                </p>
                <p className="mt-1">
                  {categorias.data?.find(
                    (categoria) => categoria.id === chamado.data.categoriaId,
                  )?.nome ?? 'Categoria'}
                </p>
              </div>
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Prioridade
                </p>
                <p className="mt-1">
                  {prioridadeTexto[chamado.data.prioridade]}
                </p>
              </div>
              {ehTi && (
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                    Responsável
                  </p>
                  <p className="mt-1">
                    {chamado.data.responsavelId
                      ? `Usuário #${chamado.data.responsavelId}`
                      : 'Sem responsável'}
                  </p>
                </div>
              )}
              <div>
                <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">
                  Aberto em
                </p>
                <p className="mt-1">{dataHora(chamado.data.criadoEm)}</p>
              </div>
            </CardContent>
          </Card>
          {chamado.data.solucao && (
            <Card className="lg:col-span-2">
              <CardContent className="p-6">
                <h3 className="font-semibold">Solução</h3>
                <p className="mt-3 whitespace-pre-wrap leading-7">
                  {chamado.data.solucao}
                </p>
              </CardContent>
            </Card>
          )}
        </div>
      )}
      {ehTi && chamado.data && categorias.data && (
        <GestaoChamado
          key={chamado.data.version}
          chamado={chamado.data}
          categorias={categorias.data}
        />
      )}
      {ehTi && chamado.data && categorias.isError && (
        <p role="alert" className="mt-6 text-red-800">
          Não foi possível carregar as opções de gestão.{' '}
          <button
            className="underline"
            onClick={() => void categorias.refetch()}
          >
            Tentar novamente
          </button>
        </p>
      )}
      {ehTi && chamado.data && (
        <HistoricoChamadoPanel chamadoId={chamado.data.id} />
      )}
    </AreaPage>
  )
}
