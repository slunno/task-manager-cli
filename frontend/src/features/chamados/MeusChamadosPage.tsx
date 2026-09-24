import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { destinoInicial, getAuthConfig, loginDev } from '../../api/auth'
import { getCategorias, getMeusChamados } from '../../api/chamados'
import { Button } from '../../components/ui/button'
import { Badge } from '../../components/ui/badge'
import { Card, CardContent } from '../../components/ui/card'
import { dataHora } from '../../lib/dataHora'
import { AreaPage } from '../auth/AreaPage'
import { ME_QUERY_KEY, useMe } from '../auth/useAuth'
import { statusTexto } from './formatacao'

function numeroPagina(valor: string | null): number {
  const numero = Number(valor)
  return Number.isSafeInteger(numero) && numero >= 0 ? numero : 0
}

export function MeusChamadosPage() {
  const sessao = useMe()
  const configuracao = useQuery({
    queryKey: ['auth-config'],
    queryFn: getAuthConfig,
    staleTime: 60_000,
    retry: false,
  })
  const cliente = useQueryClient()
  const navegar = useNavigate()
  const abrirPreviewTi = useMutation({
    mutationFn: () =>
      loginDev({ nome: 'Agente de TI', email: 'agente@exemplo.local' }),
    onSuccess: (usuario) => {
      cliente.setQueryData(ME_QUERY_KEY, usuario)
      navegar(destinoInicial(usuario.perfil), { replace: true })
    },
  })
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
      {configuracao.data?.previewDemo &&
        sessao.data?.perfil === 'FUNCIONARIO' && (
          <div className="mt-8 rounded-xl border border-cyan-200 bg-cyan-50 p-5">
            <h2 className="font-semibold">Você está na visão de funcionário</h2>
            <p className="mt-2 text-sm text-slate-700">
              Para explorar a fila e as funções da etapa E3 nesta prévia local,
              entre como agente de TI.
            </p>
            <Button
              className="mt-4"
              disabled={abrirPreviewTi.isPending}
              onClick={() => abrirPreviewTi.mutate()}
            >
              {abrirPreviewTi.isPending
                ? 'Entrando…'
                : 'Acessar prévia como agente de TI'}
            </Button>
            {abrirPreviewTi.isError && (
              <p role="alert" className="mt-3 text-sm text-red-800">
                {abrirPreviewTi.error.message}
              </p>
            )}
          </div>
        )}
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
