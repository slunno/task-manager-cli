import type { ReactNode } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router'
import { logout } from '../../api/auth'
import { Carregando } from './Guardas'
import { ME_QUERY_KEY, useMe } from './useAuth'

export function AreaPage({
  titulo,
  descricao,
  children,
}: {
  titulo: string
  descricao: string
  children?: ReactNode
}) {
  const sessao = useMe()
  const cliente = useQueryClient()
  const navegar = useNavigate()
  const sair = useMutation({
    mutationFn: logout,
    onSuccess: () => {
      cliente.setQueryData(ME_QUERY_KEY, null)
      navegar('/login', { replace: true })
    },
  })

  const usuario = sessao.data
  if (!usuario) return <Carregando />

  return (
    <div className="min-h-screen bg-[#f7f9f8] text-ink">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-4 px-5 py-5 sm:px-8">
          <Link
            className="flex items-center gap-3 font-semibold"
            to={
              usuario.perfil === 'FUNCIONARIO' ? '/meus-chamados' : '/ti/fila'
            }
          >
            <span
              aria-hidden="true"
              className="grid h-10 w-10 place-items-center rounded-xl bg-ocean text-white"
            >
              TI
            </span>
            Portal de chamados
          </Link>
          <div className="flex items-center gap-4 text-sm">
            <span className="hidden text-slate-600 sm:inline">
              {usuario.nome}
            </span>
            <button
              disabled={sair.isPending}
              onClick={() => sair.mutate()}
              className="rounded-lg border border-slate-300 px-4 py-2 font-medium hover:bg-slate-50 disabled:opacity-60"
            >
              Sair
            </button>
          </div>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-5 py-10 sm:px-8">
        <nav
          aria-label="Navegação principal"
          className="mb-8 flex flex-wrap gap-3 text-sm font-semibold"
        >
          <Link
            className="rounded-lg px-3 py-2 text-ocean hover:bg-mist"
            to="/meus-chamados"
          >
            Meus chamados
          </Link>
          <Link
            className="rounded-lg px-3 py-2 text-ocean hover:bg-mist"
            to="/chamados/novo"
          >
            Novo chamado
          </Link>
          {usuario.perfil !== 'FUNCIONARIO' && (
            <Link
              className="rounded-lg px-3 py-2 text-ocean hover:bg-mist"
              to="/ti/fila"
            >
              Fila da TI
            </Link>
          )}
        </nav>
        <p className="text-sm font-semibold uppercase tracking-[0.18em] text-ocean">
          {usuario.perfil.replace('_', ' ')}
        </p>
        <h1 className="mt-3 text-3xl font-bold">{titulo}</h1>
        <p className="mt-4 max-w-2xl text-slate-600">{descricao}</p>
        {sair.isError && (
          <p
            role="alert"
            className="mt-5 rounded-lg bg-red-50 p-3 text-red-800"
          >
            Não foi possível encerrar a sessão. Tente novamente.
          </p>
        )}
        {children ?? (
          <div className="mt-9 rounded-2xl border border-slate-200 bg-white p-6">
            <h2 className="font-semibold">Seu acesso está ativo</h2>
            <p className="mt-2 text-sm text-slate-600">
              As funcionalidades desta área serão entregues nas próximas etapas
              do portal.
            </p>
            {usuario.perfil !== 'FUNCIONARIO' && (
              <nav
                aria-label="Áreas de TI"
                className="mt-5 flex flex-wrap gap-4 text-sm font-semibold text-ocean"
              >
                <Link to="/ti/fila">Fila da TI</Link>
                <Link to="/ti/dashboard">Dashboard</Link>
                {usuario.perfil === 'TI_ADMIN' && (
                  <Link to="/ti/admin/usuarios">Administração</Link>
                )}
              </nav>
            )}
          </div>
        )}
      </main>
    </div>
  )
}
