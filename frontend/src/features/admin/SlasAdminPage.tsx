import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi, type PoliticaSla } from '../../api/admin'

export function SlasAdminPage() {
  const [edicao, setEdicao] = useState<PoliticaSla | null>(null)
  const cache = useQueryClient()
  const consulta = useQuery({
    queryKey: ['admin-slas'],
    queryFn: adminApi.slas,
  })
  const salvar = useMutation({
    mutationFn: adminApi.salvarSla,
    onSuccess: () => {
      setEdicao(null)
      void cache.invalidateQueries({ queryKey: ['admin-slas'] })
    },
  })
  return (
    <section className="mt-8 rounded-2xl border border-slate-200 bg-white p-6">
      <h2 className="text-xl font-semibold">Políticas de SLA</h2>
      <p className="mt-2 text-sm text-slate-600">
        Horas úteis por prioridade. Mudanças valem para novos prazos calculados
        após salvar.
      </p>
      {consulta.isPending && (
        <p role="status" className="mt-5">
          Carregando políticas…
        </p>
      )}
      {consulta.isError && (
        <p role="alert" className="mt-5 text-red-700">
          Falha ao carregar políticas.
        </p>
      )}
      <ul className="mt-5 divide-y divide-slate-100">
        {consulta.data?.map((politica) => (
          <li
            key={politica.prioridade}
            className="flex items-center justify-between gap-3 py-3"
          >
            <span>
              <strong>{politica.prioridade}</strong> · primeira resposta:{' '}
              {politica.horasPrimeiraResposta} h · resolução:{' '}
              {politica.horasResolucao} h
            </span>
            <button
              onClick={() => setEdicao({ ...politica })}
              className="rounded-lg border px-3 py-2 text-sm"
            >
              Editar
            </button>
          </li>
        ))}
      </ul>
      {edicao && (
        <form
          onSubmit={(e) => {
            e.preventDefault()
            salvar.mutate(edicao)
          }}
          className="mt-5 rounded-xl bg-mist p-4"
        >
          <h3 className="font-semibold">Prioridade {edicao.prioridade}</h3>
          <div className="mt-3 grid gap-3 sm:grid-cols-2">
            <label>
              Primeira resposta (horas úteis)
              <input
                type="number"
                min="1"
                max="1000"
                required
                value={edicao.horasPrimeiraResposta}
                onChange={(e) =>
                  setEdicao({
                    ...edicao,
                    horasPrimeiraResposta: Number(e.target.value),
                  })
                }
                className="mt-1 block w-full rounded-lg border p-2"
              />
            </label>
            <label>
              Resolução (horas úteis)
              <input
                type="number"
                min={edicao.horasPrimeiraResposta}
                max="1000"
                required
                value={edicao.horasResolucao}
                onChange={(e) =>
                  setEdicao({
                    ...edicao,
                    horasResolucao: Number(e.target.value),
                  })
                }
                className="mt-1 block w-full rounded-lg border p-2"
              />
            </label>
          </div>
          {salvar.isError && (
            <p role="alert" className="mt-3 text-red-700">
              {salvar.error.message}
            </p>
          )}
          <div className="mt-4 flex gap-3">
            <button
              disabled={salvar.isPending}
              className="rounded-lg bg-ocean px-4 py-2 font-semibold text-white"
            >
              Salvar
            </button>
            <button type="button" onClick={() => setEdicao(null)}>
              Cancelar
            </button>
          </div>
        </form>
      )}
    </section>
  )
}
