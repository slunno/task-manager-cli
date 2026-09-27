import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi, type CategoriaAdmin } from '../../api/admin'

export function CategoriasAdminPage() {
  const [nome, setNome] = useState('')
  const [edicao, setEdicao] = useState<CategoriaAdmin | null>(null)
  const cache = useQueryClient()
  const consulta = useQuery({
    queryKey: ['admin-categorias'],
    queryFn: adminApi.categorias,
  })
  const atualizar = useMutation({
    mutationFn: adminApi.salvarCategoria,
    onSuccess: () => {
      setEdicao(null)
      void cache.invalidateQueries({ queryKey: ['admin-categorias'] })
      void cache.invalidateQueries({ queryKey: ['categorias'] })
    },
  })
  const criar = useMutation({
    mutationFn: adminApi.criarCategoria,
    onSuccess: () => {
      setNome('')
      void cache.invalidateQueries({ queryKey: ['admin-categorias'] })
      void cache.invalidateQueries({ queryKey: ['categorias'] })
    },
  })
  return (
    <section className="mt-8 rounded-2xl border border-slate-200 bg-white p-6">
      <h2 className="text-xl font-semibold">Categorias de chamados</h2>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          criar.mutate(nome)
        }}
        className="mt-5 flex flex-wrap gap-3"
      >
        <input
          aria-label="Nova categoria"
          required
          maxLength={120}
          value={nome}
          onChange={(e) => setNome(e.target.value)}
          className="min-w-64 flex-1 rounded-lg border p-2"
          placeholder="Nome da nova categoria"
        />
        <button
          disabled={criar.isPending}
          className="rounded-lg bg-ocean px-4 py-2 font-semibold text-white"
        >
          Adicionar
        </button>
      </form>
      {criar.isError && (
        <p role="alert" className="mt-3 text-red-700">
          {criar.error.message}
        </p>
      )}
      {consulta.isPending && (
        <p role="status" className="mt-5">
          Carregando categorias…
        </p>
      )}
      {consulta.isError && (
        <p role="alert" className="mt-5 text-red-700">
          Falha ao carregar categorias.
        </p>
      )}
      <ul className="mt-5 divide-y divide-slate-100">
        {consulta.data?.map((categoria) => (
          <li
            key={categoria.id}
            className="flex items-center justify-between gap-3 py-3"
          >
            <span>
              {categoria.nome}{' '}
              <small className="text-slate-500">
                ({categoria.ativa ? 'ativa' : 'inativa'})
              </small>
            </span>
            <button
              className="rounded-lg border px-3 py-2 text-sm"
              onClick={() => setEdicao({ ...categoria })}
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
            atualizar.mutate(edicao)
          }}
          className="mt-5 rounded-xl bg-mist p-4"
        >
          <h3 className="font-semibold">Editar categoria</h3>
          <input
            aria-label="Nome da categoria"
            required
            maxLength={120}
            value={edicao.nome}
            onChange={(e) => setEdicao({ ...edicao, nome: e.target.value })}
            className="mt-3 block w-full rounded-lg border p-2"
          />
          <label className="mt-3 flex gap-2">
            <input
              type="checkbox"
              checked={edicao.ativa}
              onChange={(e) =>
                setEdicao({ ...edicao, ativa: e.target.checked })
              }
            />{' '}
            Ativa para novos chamados
          </label>
          {atualizar.isError && (
            <p role="alert" className="mt-3 text-red-700">
              {atualizar.error.message}
            </p>
          )}
          <div className="mt-4 flex gap-3">
            <button
              disabled={atualizar.isPending}
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
