import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi, type Setor } from '../../api/admin'

export function SetoresAdminPage() {
  const [nome, setNome] = useState('')
  const [edicao, setEdicao] = useState<Setor | null>(null)
  const cache = useQueryClient()
  const setores = useQuery({
    queryKey: ['admin-setores'],
    queryFn: adminApi.todosSetores,
  })
  const atualizarLista = () => {
    void cache.invalidateQueries({ queryKey: ['admin-setores'] })
    void cache.invalidateQueries({ queryKey: ['setores'] })
  }
  const criar = useMutation({
    mutationFn: adminApi.criarSetor,
    onSuccess: () => {
      setNome('')
      atualizarLista()
    },
  })
  const salvar = useMutation({
    mutationFn: adminApi.salvarSetor,
    onSuccess: () => {
      setEdicao(null)
      atualizarLista()
    },
  })

  function enviarNovo(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    criar.mutate(nome.trim())
  }

  return (
    <section className="mt-8 space-y-6">
      <form
        onSubmit={enviarNovo}
        className="rounded-2xl border border-slate-200 bg-white p-6"
      >
        <h2 className="text-xl font-semibold">Setores</h2>
        <p className="mt-2 text-sm text-slate-600">
          Organize os usuários por setor para acompanhar os relatórios.
        </p>
        <label className="mt-4 block text-sm font-semibold">
          Nome do setor
          <input
            required
            maxLength={120}
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            className="mt-1 block w-full rounded-lg border p-2"
          />
        </label>
        <button
          disabled={criar.isPending}
          className="mt-4 rounded-lg bg-ocean px-4 py-2 font-semibold text-white"
        >
          Adicionar
        </button>
        {criar.isError && (
          <p role="alert" className="mt-3 text-red-700">
            {criar.error.message}
          </p>
        )}
      </form>
      <div className="rounded-2xl border border-slate-200 bg-white p-6">
        <h3 className="font-semibold">Setores cadastrados</h3>
        {setores.isPending && (
          <p role="status" className="mt-3">
            Carregando setores…
          </p>
        )}
        {setores.isError && (
          <p role="alert" className="mt-3 text-red-700">
            {setores.error.message}
          </p>
        )}
        <ul className="mt-4 divide-y divide-slate-100">
          {setores.data?.map((setor) => (
            <li
              key={setor.id}
              className="flex items-center justify-between gap-3 py-3"
            >
              <span>
                {setor.nome}{' '}
                <span className="text-sm text-slate-600">
                  · {setor.ativo ? 'Ativo' : 'Inativo'}
                </span>
              </span>
              <button
                type="button"
                className="rounded-lg border px-3 py-2 text-sm"
                onClick={() => setEdicao({ ...setor })}
              >
                Editar
              </button>
            </li>
          ))}
        </ul>
        {setores.data?.length === 0 && (
          <p className="mt-3 text-slate-600">Nenhum setor cadastrado.</p>
        )}
      </div>
      {edicao && (
        <form
          onSubmit={(evento) => {
            evento.preventDefault()
            salvar.mutate(edicao)
          }}
          className="rounded-2xl border border-slate-200 bg-white p-6"
        >
          <h3 className="font-semibold">Editar setor</h3>
          <label className="mt-4 block text-sm font-semibold">
            Nome
            <input
              required
              maxLength={120}
              value={edicao.nome}
              onChange={(e) => setEdicao({ ...edicao, nome: e.target.value })}
              className="mt-1 block w-full rounded-lg border p-2"
            />
          </label>
          <label className="mt-4 flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={edicao.ativo}
              onChange={(e) =>
                setEdicao({ ...edicao, ativo: e.target.checked })
              }
            />{' '}
            Ativo
          </label>
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
