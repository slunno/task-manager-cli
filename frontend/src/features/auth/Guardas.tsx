import { Navigate, useLocation } from 'react-router'
import { destinoInicial, type Perfil } from '../../api/auth'
import { useMe } from './useAuth'

export function Carregando() {
  return (
    <main
      className="grid min-h-screen place-items-center p-6"
      aria-live="polite"
    >
      <p className="text-slate-600">Carregando sua sessão…</p>
    </main>
  )
}

export function ErroSessao({ refazer }: { refazer: () => void }) {
  return (
    <main className="grid min-h-screen place-items-center p-6">
      <div className="max-w-md text-center">
        <h1 className="text-2xl font-bold">
          Não foi possível carregar sua sessão
        </h1>
        <p className="mt-3 text-slate-600">
          Verifique sua conexão e tente novamente.
        </p>
        <button
          className="mt-6 rounded-lg bg-ocean px-5 py-3 font-semibold text-white"
          onClick={refazer}
        >
          Tentar novamente
        </button>
      </div>
    </main>
  )
}

export function Entrada() {
  const sessao = useMe()
  if (sessao.isPending) return <Carregando />
  if (sessao.isError)
    return <ErroSessao refazer={() => void sessao.refetch()} />
  return (
    <Navigate
      to={sessao.data ? destinoInicial(sessao.data.perfil) : '/login'}
      replace
    />
  )
}

export function ExigirPerfil({
  permitido,
  children,
}: {
  permitido: Perfil[]
  children: React.ReactNode
}) {
  const sessao = useMe()
  const local = useLocation()
  if (sessao.isPending) return <Carregando />
  if (sessao.isError)
    return <ErroSessao refazer={() => void sessao.refetch()} />
  if (!sessao.data)
    return <Navigate to="/login" state={{ origem: local.pathname }} replace />
  if (!permitido.includes(sessao.data.perfil)) {
    return <Navigate to={destinoInicial(sessao.data.perfil)} replace />
  }
  return <>{children}</>
}
