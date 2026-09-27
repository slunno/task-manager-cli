import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi, type Janela } from '../../api/admin'

const dias = [
  'Segunda',
  'Terça',
  'Quarta',
  'Quinta',
  'Sexta',
  'Sábado',
  'Domingo',
]

export function CalendarioAdminPage() {
  const [janelas, setJanelas] = useState<Janela[]>([])
  const [data, setData] = useState('')
  const [descricao, setDescricao] = useState('')
  const cache = useQueryClient()
  const consulta = useQuery({
    queryKey: ['admin-calendario'],
    queryFn: adminApi.calendario,
  })
  useEffect(() => {
    if (consulta.data) setJanelas(consulta.data.expediente)
  }, [consulta.data])
  const salvar = useMutation({
    mutationFn: adminApi.salvarExpediente,
    onSuccess: () =>
      void cache.invalidateQueries({ queryKey: ['admin-calendario'] }),
  })
  const adicionar = useMutation({
    mutationFn: () => adminApi.adicionarFeriado(data, descricao),
    onSuccess: () => {
      setData('')
      setDescricao('')
      void cache.invalidateQueries({ queryKey: ['admin-calendario'] })
    },
  })
  const remover = useMutation({
    mutationFn: adminApi.removerFeriado,
    onSuccess: () =>
      void cache.invalidateQueries({ queryKey: ['admin-calendario'] }),
  })
  const alterar = (
    indice: number,
    campo: keyof Janela,
    valor: string | number,
  ) =>
    setJanelas(
      janelas.map((janela, i) =>
        i === indice ? { ...janela, [campo]: valor } : janela,
      ),
    )

  return (
    <section className="mt-8 space-y-6">
      <div className="rounded-2xl border border-slate-200 bg-white p-6">
        <h2 className="text-xl font-semibold">Expediente</h2>
        <p className="mt-2 text-sm text-slate-600">
          Horário de São Paulo. Use janelas sem sobreposição; ao menos uma deve
          permanecer ativa.
        </p>
        {consulta.isPending && (
          <p role="status" className="mt-4">
            Carregando calendário…
          </p>
        )}
        {consulta.isError && (
          <p role="alert" className="mt-4 text-red-700">
            Falha ao carregar calendário.
          </p>
        )}
        <form
          onSubmit={(e) => {
            e.preventDefault()
            salvar.mutate(janelas)
          }}
          className="mt-5 space-y-3"
        >
          {janelas.map((janela, indice) => (
            <div
              key={indice}
              className="flex flex-wrap items-end gap-3 rounded-lg bg-mist p-3"
            >
              <label>
                Dia
                <select
                  value={janela.diaSemana}
                  onChange={(e) =>
                    alterar(indice, 'diaSemana', Number(e.target.value))
                  }
                  className="mt-1 block rounded-lg border p-2"
                >
                  {dias.map((dia, i) => (
                    <option key={dia} value={i + 1}>
                      {dia}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Início
                <input
                  type="time"
                  required
                  value={janela.inicio.slice(0, 5)}
                  onChange={(e) => alterar(indice, 'inicio', e.target.value)}
                  className="mt-1 block rounded-lg border p-2"
                />
              </label>
              <label>
                Fim
                <input
                  type="time"
                  required
                  value={janela.fim.slice(0, 5)}
                  onChange={(e) => alterar(indice, 'fim', e.target.value)}
                  className="mt-1 block rounded-lg border p-2"
                />
              </label>
              <button
                type="button"
                onClick={() =>
                  setJanelas(janelas.filter((_, i) => i !== indice))
                }
                className="rounded-lg border px-3 py-2"
              >
                Remover
              </button>
            </div>
          ))}
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() =>
                setJanelas([
                  ...janelas,
                  { diaSemana: 1, inicio: '09:00', fim: '18:00' },
                ])
              }
              className="rounded-lg border px-4 py-2"
            >
              Adicionar janela
            </button>
            <button
              disabled={salvar.isPending || janelas.length === 0}
              className="rounded-lg bg-ocean px-4 py-2 font-semibold text-white disabled:opacity-50"
            >
              Salvar expediente
            </button>
          </div>
          {salvar.isError && (
            <p role="alert" className="text-red-700">
              {salvar.error.message}
            </p>
          )}
          {salvar.isSuccess && (
            <p role="status" className="text-green-700">
              Expediente atualizado.
            </p>
          )}
        </form>
      </div>
      <div className="rounded-2xl border border-slate-200 bg-white p-6">
        <h2 className="text-xl font-semibold">Feriados</h2>
        <form
          onSubmit={(e) => {
            e.preventDefault()
            adicionar.mutate()
          }}
          className="mt-5 flex flex-wrap items-end gap-3"
        >
          <label>
            Data
            <input
              type="date"
              required
              value={data}
              onChange={(e) => setData(e.target.value)}
              className="mt-1 block rounded-lg border p-2"
            />
          </label>
          <label className="flex-1">
            Descrição
            <input
              required
              maxLength={180}
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              className="mt-1 block w-full rounded-lg border p-2"
            />
          </label>
          <button
            disabled={adicionar.isPending}
            className="rounded-lg bg-ocean px-4 py-2 font-semibold text-white"
          >
            Adicionar
          </button>
        </form>
        {adicionar.isError && (
          <p role="alert" className="mt-3 text-red-700">
            {adicionar.error.message}
          </p>
        )}
        {remover.isError && (
          <p role="alert" className="mt-3 text-red-700">
            {remover.error.message}
          </p>
        )}
        <ul className="mt-5 divide-y divide-slate-100">
          {consulta.data?.feriados.map((feriado) => (
            <li
              key={feriado.id}
              className="flex items-center justify-between gap-3 py-3"
            >
              <span>
                {feriado.data} · {feriado.descricao}
              </span>
              <button
                disabled={remover.isPending}
                onClick={() => remover.mutate(feriado.id)}
                className="rounded-lg border px-3 py-2 text-sm"
              >
                Remover
              </button>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
