import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { getHistorico, type HistoricoChamado } from '../../api/chamados'
import { Button } from '../../components/ui/button'
import { Card, CardContent, CardHeader } from '../../components/ui/card'
import { dataHora } from '../../lib/dataHora'
import { prioridadeTexto, statusTexto } from './formatacao'

const campoTexto: Record<string, string> = {
  responsavel: 'Responsável',
  status: 'Status',
  prioridade: 'Prioridade',
  categoria: 'Categoria',
}

function valor(registro: HistoricoChamado, conteudo?: string | null): string {
  if (!conteudo) return 'Sem responsável'
  if (registro.campo === 'status' && conteudo in statusTexto)
    return statusTexto[conteudo as keyof typeof statusTexto]
  if (registro.campo === 'prioridade' && conteudo in prioridadeTexto)
    return prioridadeTexto[conteudo as keyof typeof prioridadeTexto]
  if (registro.campo === 'responsavel' || registro.campo === 'categoria')
    return `#${conteudo}`
  return conteudo
}

export function HistoricoChamadoPanel({ chamadoId }: { chamadoId: number }) {
  const [pagina, setPagina] = useState(0)
  const historico = useQuery({
    queryKey: ['historico', chamadoId, pagina],
    queryFn: () => getHistorico(chamadoId, pagina),
    retry: false,
  })

  return (
    <Card className="mt-6">
      <CardHeader>
        <h2 className="text-xl font-semibold">Histórico de alterações</h2>
      </CardHeader>
      <CardContent>
        {historico.isPending && <p role="status">Carregando histórico…</p>}
        {historico.isError && (
          <div role="alert" className="rounded-lg bg-red-50 p-4 text-red-800">
            Não foi possível carregar o histórico.
            <Button
              variant="outline"
              className="ml-3"
              onClick={() => void historico.refetch()}
            >
              Tentar novamente
            </Button>
          </div>
        )}
        {historico.data?.content.length === 0 && (
          <p className="text-sm text-slate-600">
            Ainda não há alterações neste chamado.
          </p>
        )}
        {historico.data && historico.data.content.length > 0 && (
          <>
            <ol className="space-y-4 border-l-2 border-slate-200 pl-5">
              {historico.data.content.map((registro) => (
                <li key={registro.id} className="relative text-sm">
                  <span
                    aria-hidden="true"
                    className="absolute -left-[26px] top-1.5 h-2.5 w-2.5 rounded-full bg-ocean"
                  />
                  <p className="font-semibold">
                    {campoTexto[registro.campo ?? ''] ?? registro.campo}
                  </p>
                  <p className="mt-1 text-slate-700">
                    {valor(registro, registro.valorAnterior)} →{' '}
                    {valor(registro, registro.valorNovo)}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">
                    {registro.criadoEm ? dataHora(registro.criadoEm) : ''} ·
                    usuário #{registro.usuarioId}
                  </p>
                </li>
              ))}
            </ol>
            <div className="mt-6 flex items-center justify-between gap-3 text-sm">
              <Button
                variant="outline"
                disabled={pagina === 0}
                onClick={() => setPagina(pagina - 1)}
              >
                Anterior
              </Button>
              <span>
                Página {pagina + 1} de {historico.data.totalPages}
              </span>
              <Button
                variant="outline"
                disabled={pagina + 1 >= historico.data.totalPages}
                onClick={() => setPagina(pagina + 1)}
              >
                Próxima
              </Button>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}
