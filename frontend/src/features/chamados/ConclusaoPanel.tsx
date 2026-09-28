import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  avaliar,
  avaliacaoAusente,
  getAvaliacao,
  reabrir,
} from '../../api/conclusao'
import type { Chamado } from '../../api/chamados'
import { dataHora } from '../../lib/dataHora'

export function ConclusaoPanel({ chamado }: { chamado: Chamado }) {
  const [nota, setNota] = useState(5)
  const [comentario, setComentario] = useState('')
  const cache = useQueryClient()
  const consulta = useQuery({
    queryKey: ['avaliacao', chamado.id],
    queryFn: () => getAvaliacao(chamado.id),
    retry: false,
  })
  const enviar = useMutation({
    mutationFn: () => avaliar(chamado.id, nota, comentario),
    onSuccess: () =>
      void cache.invalidateQueries({ queryKey: ['avaliacao', chamado.id] }),
  })
  const reabertura = useMutation({
    mutationFn: () => reabrir(chamado.id, chamado.version),
    onSuccess: () => {
      void cache.invalidateQueries({ queryKey: ['chamado', chamado.id] })
      void cache.invalidateQueries({ queryKey: ['avaliacao', chamado.id] })
      void cache.invalidateQueries({ queryKey: ['meus-chamados'] })
    },
  })

  return (
    <section
      id="avaliacao"
      className="mt-6 rounded-2xl border border-slate-200 bg-white p-6"
    >
      <h2 className="text-xl font-semibold">Conclusão do atendimento</h2>
      {chamado.resolvidoEm && (
        <p className="mt-2 text-sm text-slate-600">
          Resolvido em {dataHora(chamado.resolvidoEm)}.
        </p>
      )}
      {chamado.fechadoEm && (
        <p className="mt-1 text-sm text-slate-600">
          Fechado em {dataHora(chamado.fechadoEm)}.
        </p>
      )}
      {chamado.status === 'RESOLVIDO' && (
        <div className="mt-5">
          <p className="text-sm text-slate-600">
            Se o problema persistir, reabra este chamado dentro do prazo
            configurado pela TI.
          </p>
          <button
            disabled={reabertura.isPending}
            onClick={() => reabertura.mutate()}
            className="mt-3 rounded-lg border border-ocean px-4 py-2 font-semibold text-ocean disabled:opacity-50"
          >
            Reabrir chamado
          </button>
          {reabertura.isError && (
            <p role="alert" className="mt-3 text-red-700">
              {reabertura.error.message}
            </p>
          )}
        </div>
      )}
      {consulta.isPending && (
        <p role="status" className="mt-5">
          Carregando avaliação…
        </p>
      )}
      {consulta.data && (
        <div className="mt-5 rounded-xl bg-mist p-4">
          <p className="font-semibold">
            Sua avaliação: {consulta.data.nota} de 5
          </p>
          {consulta.data.comentario && (
            <p className="mt-2 whitespace-pre-wrap">
              {consulta.data.comentario}
            </p>
          )}
        </div>
      )}
      {consulta.isError && !avaliacaoAusente(consulta.error) && (
        <p role="alert" className="mt-5 text-red-700">
          Não foi possível carregar a avaliação.{' '}
          <button className="underline" onClick={() => void consulta.refetch()}>
            Tentar novamente
          </button>
        </p>
      )}
      {consulta.isError && avaliacaoAusente(consulta.error) && (
        <form
          onSubmit={(e) => {
            e.preventDefault()
            enviar.mutate()
          }}
          className="mt-6 space-y-4"
        >
          <h3 className="font-semibold">Avalie o atendimento</h3>
          <label className="block">
            Nota
            <select
              value={nota}
              onChange={(e) => setNota(Number(e.target.value))}
              className="mt-1 block w-full rounded-lg border p-2"
            >
              {[1, 2, 3, 4, 5].map((valor) => (
                <option key={valor} value={valor}>
                  {valor} de 5
                </option>
              ))}
            </select>
          </label>
          <label className="block">
            Comentário opcional
            <textarea
              maxLength={1000}
              rows={4}
              value={comentario}
              onChange={(e) => setComentario(e.target.value)}
              className="mt-1 block w-full rounded-lg border p-2"
            />
          </label>
          <button
            disabled={enviar.isPending}
            className="rounded-lg bg-ocean px-4 py-2 font-semibold text-white disabled:opacity-50"
          >
            Enviar avaliação
          </button>
          {enviar.isError && (
            <p role="alert" className="text-red-700">
              {enviar.error.message}
            </p>
          )}
        </form>
      )}
    </section>
  )
}
