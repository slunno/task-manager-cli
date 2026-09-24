import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'
import { getCategorias, getMeusChamados } from '../../api/chamados'
import { Button } from '../../components/ui/button'
import { Badge } from '../../components/ui/badge'
import { Card, CardContent } from '../../components/ui/card'
import { dataHora } from '../../lib/dataHora'
import { AreaPage } from '../auth/AreaPage'
import { statusTexto } from './formatacao'

function numeroPagina(valor: string | null): number {
  const numero = Number(valor)
  return Number.isSafeInteger(numero) && numero >= 0 ? numero : 0
}

export function MeusChamadosPage() {
  const [parametros, setParametros] = useSearchParams()
  const pagina = numeroPagina(parametros.get('page'))
  const lista = useQuery({
    queryKey: ['meus-chamados', pagina],
    queryFn: () => getMeusChamados(pagina),
    retry: false,
  })
  const categorias = useQuery({
    queryKey: ['categorias'],
    queryFn: getCategorias,
    staleTime: 60_000,
    retry: false,
  })

  function mudarPagina(destino: number) {
    setParametros({ page: String(destino) })
  }

  return (
    <AreaPage
      titulo="Meus chamados"
      descricao="Acompanhe seus pedidos de ajuda em um só lugar."
    >
      <div className="mt-8 flex justify-end">
        <Button asChild size="lg">
          <Link to="/chamados/novo">Abrir chamado</Link>
        </Button>
      </div>
      {lista.isPending && (
        <p role="status" className="mt-8 text-slate-600">
          Carregando chamados…
        </p>
      )}
      {lista.isError && (
        <div
          role="alert"
          className="mt-8 rounded-xl border border-red-200 bg-red-50 p-5 text-red-800"
        >
          <p>Não foi possível carregar seus chamados.</p>
          <Button
            className="mt-4"
            variant="outline"
            onClick={() => void lista.refetch()}
          >
            Tentar novamente
          </Button>
        </div>
      )}
      {lista.data?.content.length === 0 && (
        <Card className="mt-8">
          <CardContent className="py-12 text-center">
            <h2 className="text-xl font-semibold">Nenhum chamado por aqui</h2>
            <p className="mt-2 text-slate-600">
              Quando precisar de ajuda, abra um chamado. Leva menos de um
              minuto.
            </p>
          </CardContent>
        </Card>
      )}
      {lista.data && lista.data.content.length > 0 && (
        <>
          <ul className="mt-6 space-y-3" aria-label="Lista de chamados">
            {lista.data.content.map((chamado) => (
              <li key={chamado.id}>
                <Card className="transition-shadow hover:shadow-md">
                  <CardContent className="p-5">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div>
                        <p className="text-xs font-semibold tracking-wide text-ocean">
                          {chamado.numero}
                        </p>
                        <Link
                          className="mt-1 inline-block text-lg font-semibold hover:underline"
                          to={`/chamados/${chamado.id}`}
                        >
                          {chamado.titulo}
                        </Link>
                      </div>
                      <Badge variant="secondary">
                        {statusTexto[chamado.status]}
                      </Badge>
                    </div>
                    <p className="mt-3 text-sm text-slate-600">
                      {categorias.data?.find(
                        (categoria) => categoria.id === chamado.categoriaId,
                      )?.nome ?? 'Categoria'}
                      {' · '}
                      {dataHora(chamado.criadoEm)}
                    </p>
                  </CardContent>
                </Card>
              </li>
            ))}
          </ul>
          <div className="mt-6 flex items-center justify-between gap-3 text-sm">
            <Button
              variant="outline"
              disabled={pagina === 0}
              onClick={() => mudarPagina(pagina - 1)}
            >
              Anterior
            </Button>
            <span>
              Página {pagina + 1} de {lista.data.totalPages}
            </span>
            <Button
              variant="outline"
              disabled={pagina + 1 >= lista.data.totalPages}
              onClick={() => mudarPagina(pagina + 1)}
            >
              Próxima
            </Button>
          </div>
        </>
      )}
    </AreaPage>
  )
}
