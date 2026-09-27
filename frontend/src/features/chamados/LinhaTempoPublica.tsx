import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { getLinhaTempo } from '../../api/interacoes'
import { Button } from '../../components/ui/button'
import { dataHora } from '../../lib/dataHora'
import { statusTexto } from './formatacao'

export function LinhaTempoPublica({
  chamadoId,
  usuarioId,
}: {
  chamadoId: number
  usuarioId: number
}) {
  const [pagina, setPagina] = useState(0)
  const atividade = useQuery({
    queryKey: ['linha-tempo', chamadoId, pagina],
    queryFn: () => getLinhaTempo(chamadoId, pagina),
    retry: false,
  })

  if (atividade.isPending) return <p role="status">Carregando andamento…</p>
  if (atividade.isError)
    return (
      <p role="alert" className="text-red-800">
        Não foi possível carregar o andamento.{' '}
        <button className="underline" onClick={() => void atividade.refetch()}>
          Tentar novamente
        </button>
      </p>
    )
  if (atividade.data.content.length === 0)
    return <p className="text-sm text-slate-600">Ainda não há atualizações.</p>

  return (
    <>
      <ol className="space-y-4">
        {atividade.data.content.map((item) => (
          <li
            key={`${item.tipo}-${item.id}`}
            className="rounded-xl bg-slate-50 p-4"
          >
            <p className="text-sm font-semibold">
              {item.tipo === 'STATUS'
                ? 'Status atualizado'
                : item.autorId === usuarioId
                  ? 'Você'
                  : 'Equipe de TI'}
            </p>
            <p className="mt-2 whitespace-pre-wrap break-words">
              {item.tipo === 'STATUS' && item.status
                ? (statusTexto[item.status as keyof typeof statusTexto] ??
                  item.status)
                : item.texto}
            </p>
            <time
              className="mt-2 block text-xs text-slate-500"
              dateTime={item.criadoEm}
            >
              {dataHora(item.criadoEm)}
            </time>
          </li>
        ))}
      </ol>
      {atividade.data.totalPages > 1 && (
        <div className="mt-5 flex items-center justify-between text-sm">
          <Button
            variant="outline"
            disabled={pagina === 0}
            onClick={() => setPagina(pagina - 1)}
          >
            Anterior
          </Button>
          <span>
            Página {pagina + 1} de {atividade.data.totalPages}
          </span>
          <Button
            variant="outline"
            disabled={pagina + 1 >= atividade.data.totalPages}
            onClick={() => setPagina(pagina + 1)}
          >
            Próxima
          </Button>
        </div>
      )}
    </>
  )
}
