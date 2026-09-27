import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminApi, type UsuarioAdmin } from '../../api/admin'

const perfis = ['FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN'] as const

export function UsuariosAdminPage() {
  const [page, setPage] = useState(0)
  const [edicao, setEdicao] = useState<UsuarioAdmin | null>(null)
  const [arquivo, setArquivo] = useState<File | null>(null)
  const cache = useQueryClient()
  const consulta = useQuery({
    queryKey: ['admin-usuarios', page],
    queryFn: () => adminApi.usuarios(page),
  })
  const setores = useQuery({
    queryKey: ['admin-setores'],
    queryFn: adminApi.todosSetores,
  })
  const salvar = useMutation({
    mutationFn: adminApi.salvarUsuario,
    onSuccess: () => {
      setEdicao(null)
      void cache.invalidateQueries({ queryKey: ['admin-usuarios'] })
    },
  })
  const importar = useMutation({
    mutationFn: adminApi.importarUsuarios,
    onSuccess: () =>
      void cache.invalidateQueries({ queryKey: ['admin-usuarios'] }),
  })

  return (
    <section className="mt-8 space-y-6">
      <div className="rounded-2xl border border-slate-200 bg-white p-6">
        <h2 className="text-xl font-semibold">Usuários</h2>
        <p className="mt-2 text-sm text-slate-600">
          Altere perfil e acesso de cada pessoa. O último administrador ativo
          fica protegido.
        </p>
        {consulta.isPending && (
          <p role="status" className="mt-4">
            Carregando usuários…
          </p>
        )}
        {consulta.isError && (
          <p role="alert" className="mt-4 text-red-700">
            Falha ao carregar usuários.
          </p>
        )}
        <ul className="mt-5 divide-y divide-slate-100">
          {consulta.data?.content.map((usuario) => (
            <li
              key={usuario.id}
              className="flex flex-wrap items-center justify-between gap-3 py-3"
            >
              <div>
                <strong>{usuario.nome}</strong>
                <p className="text-sm text-slate-600">
                  {usuario.email} · {usuario.perfil} ·{' '}
                  {usuario.ativo ? 'Ativo' : 'Inativo'}
                  {usuario.setorId
                    ? ` · ${setores.data?.find((setor) => setor.id === usuario.setorId)?.nome ?? 'Setor não encontrado'}`
                    : ''}
                </p>
              </div>
              <button
                className="rounded-lg border border-slate-300 px-3 py-2 text-sm"
                onClick={() => setEdicao({ ...usuario })}
              >
                Editar
              </button>
            </li>
          ))}
        </ul>
        {consulta.data && (
          <div className="mt-4 flex items-center gap-3 text-sm">
            <button
              disabled={page === 0}
              onClick={() => setPage(page - 1)}
              className="disabled:opacity-40"
            >
              Anterior
            </button>
            <span>
              Página {page + 1} de {Math.max(1, consulta.data.totalPages)}
            </span>
            <button
              disabled={page + 1 >= consulta.data.totalPages}
              onClick={() => setPage(page + 1)}
              className="disabled:opacity-40"
            >
              Próxima
            </button>
          </div>
        )}
      </div>
      {edicao && (
        <form
          className="rounded-2xl border border-slate-200 bg-white p-6"
          onSubmit={(evento) => {
            evento.preventDefault()
            salvar.mutate(edicao)
          }}
        >
          <h3 className="font-semibold">Editar usuário</h3>
          <div className="mt-4 grid gap-4 sm:grid-cols-2">
            <label>
              Nome
              <input
                required
                maxLength={180}
                value={edicao.nome}
                onChange={(e) => setEdicao({ ...edicao, nome: e.target.value })}
                className="mt-1 block w-full rounded-lg border p-2"
              />
            </label>
            <label>
              E-mail
              <input
                required
                type="email"
                value={edicao.email}
                onChange={(e) =>
                  setEdicao({ ...edicao, email: e.target.value })
                }
                className="mt-1 block w-full rounded-lg border p-2"
              />
            </label>
            <label>
              Perfil
              <select
                value={edicao.perfil}
                onChange={(e) =>
                  setEdicao({
                    ...edicao,
                    perfil: e.target.value as UsuarioAdmin['perfil'],
                  })
                }
                className="mt-1 block w-full rounded-lg border p-2"
              >
                {perfis.map((perfil) => (
                  <option key={perfil}>{perfil}</option>
                ))}
              </select>
            </label>
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={edicao.ativo}
                onChange={(e) =>
                  setEdicao({ ...edicao, ativo: e.target.checked })
                }
              />{' '}
              Acesso ativo
            </label>
            <label>
              Setor
              <select
                value={edicao.setorId ?? ''}
                onChange={(e) =>
                  setEdicao({
                    ...edicao,
                    setorId: e.target.value ? Number(e.target.value) : null,
                  })
                }
                className="mt-1 block w-full rounded-lg border p-2"
              >
                <option value="">Sem setor</option>
                {setores.data
                  ?.filter(
                    (setor) => setor.ativo || setor.id === edicao.setorId,
                  )
                  .map((setor) => (
                    <option key={setor.id} value={setor.id}>
                      {setor.nome}
                      {setor.ativo ? '' : ' (inativo)'}
                    </option>
                  ))}
              </select>
            </label>
          </div>
          {salvar.isError && (
            <p role="alert" className="mt-3 text-red-700">
              {salvar.error.message}
            </p>
          )}
          <div className="mt-5 flex gap-3">
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
      <form
        className="rounded-2xl border border-slate-200 bg-white p-6"
        onSubmit={(evento) => {
          evento.preventDefault()
          if (arquivo) importar.mutate(arquivo)
        }}
      >
        <h2 className="text-xl font-semibold">Importar CSV</h2>
        <p className="mt-2 text-sm text-slate-600">
          Cabeçalho: nome;email;perfil;ativo. Até 1 MB e 1.000 registros. Ativo:
          true ou false. Linhas inválidas aparecem no relatório.
        </p>
        <input
          aria-label="Arquivo CSV"
          className="mt-4 block"
          type="file"
          accept=".csv,text/csv"
          required
          onChange={(evento) => setArquivo(evento.target.files?.[0] ?? null)}
        />
        <button
          disabled={!arquivo || importar.isPending}
          className="mt-4 rounded-lg bg-ocean px-4 py-2 font-semibold text-white disabled:opacity-50"
        >
          Importar
        </button>
        {importar.isError && (
          <p role="alert" className="mt-3 text-red-700">
            {importar.error.message}
          </p>
        )}
        {importar.data && (
          <div role="status" className="mt-4">
            <p>
              {importar.data.importados} importados · {importar.data.rejeitados}{' '}
              rejeitados
            </p>
            <ul className="mt-2 list-inside list-disc text-sm text-red-700">
              {importar.data.erros.map((erro) => (
                <li key={erro.linha}>
                  Linha {erro.linha}: {erro.motivo}
                </li>
              ))}
            </ul>
          </div>
        )}
      </form>
    </section>
  )
}
