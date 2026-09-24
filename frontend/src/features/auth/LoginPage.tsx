import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Navigate, useNavigate } from 'react-router'
import { z } from 'zod'
import { destinoInicial, getAuthConfig, loginDev } from '../../api/auth'
import { Carregando } from './Guardas'
import { ME_QUERY_KEY, useMe } from './useAuth'

const esquemaLogin = z.object({
  email: z.email('Informe um e-mail válido'),
  nome: z.string().trim().min(1, 'Informe seu nome').max(180),
})

type CamposLogin = z.infer<typeof esquemaLogin>

export function LoginPage() {
  const sessao = useMe()
  const config = useQuery({
    queryKey: ['auth-config'],
    queryFn: getAuthConfig,
    retry: false,
  })
  const cliente = useQueryClient()
  const navegar = useNavigate()
  const formulario = useForm<CamposLogin>({
    resolver: zodResolver(esquemaLogin),
    defaultValues: { email: '', nome: '' },
  })
  const entrar = useMutation({
    mutationFn: loginDev,
    onSuccess: (usuario) => {
      cliente.setQueryData(ME_QUERY_KEY, usuario)
      navegar(destinoInicial(usuario.perfil), { replace: true })
    },
  })

  if (sessao.isPending || config.isPending) return <Carregando />
  if (sessao.data)
    return <Navigate to={destinoInicial(sessao.data.perfil)} replace />

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#f7f9f8] px-5 py-12 text-ink">
      <div className="grid w-full max-w-4xl overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl md:grid-cols-2">
        <section className="bg-ink p-8 text-white sm:p-10">
          <span className="inline-grid h-11 w-11 place-items-center rounded-xl bg-ocean font-bold">
            TI
          </span>
          <p className="mt-16 text-sm font-semibold uppercase tracking-[0.2em] text-cyan-200">
            Portal interno
          </p>
          <h1 className="mt-4 text-3xl font-bold leading-tight sm:text-4xl">
            A ajuda de TI em um só lugar.
          </h1>
          <p className="mt-5 leading-7 text-slate-300">
            Entre para acompanhar seus pedidos e conversar com a equipe de
            atendimento.
          </p>
        </section>
        <section className="p-8 sm:p-10">
          <h2 className="text-2xl font-bold">Acessar o portal</h2>
          <p className="mt-2 text-sm text-slate-600">
            Sua conta determina os recursos disponíveis.
          </p>
          {config.isError || sessao.isError ? (
            <div
              role="alert"
              className="mt-6 rounded-xl bg-red-50 p-4 text-red-800"
            >
              Não foi possível verificar o acesso. Atualize a página e tente
              novamente.
            </div>
          ) : config.data?.modo === 'oidc' ? (
            <a
              href={config.data.urlLogin ?? '#'}
              className="mt-8 inline-flex min-h-11 w-full items-center justify-center rounded-xl bg-ocean px-5 py-3 font-semibold text-white hover:bg-[#095a6b]"
            >
              Entrar com conta corporativa
            </a>
          ) : config.data?.modo === 'dev' ? (
            <form
              className="mt-7 space-y-5"
              onSubmit={formulario.handleSubmit((dados) =>
                entrar.mutate(dados),
              )}
              noValidate
            >
              <p className="rounded-lg bg-amber-50 px-4 py-3 text-sm text-amber-900">
                Acesso simulado para desenvolvimento local.
              </p>
              <div>
                <label
                  className="mb-2 block text-sm font-medium"
                  htmlFor="nome"
                >
                  Nome
                </label>
                <input
                  id="nome"
                  autoComplete="name"
                  className="w-full rounded-xl border border-slate-300 px-4 py-3"
                  {...formulario.register('nome')}
                  aria-invalid={!!formulario.formState.errors.nome}
                />
                {formulario.formState.errors.nome && (
                  <p role="alert" className="mt-1 text-sm text-red-700">
                    {formulario.formState.errors.nome.message}
                  </p>
                )}
              </div>
              <div>
                <label
                  className="mb-2 block text-sm font-medium"
                  htmlFor="email"
                >
                  E-mail corporativo
                </label>
                <input
                  id="email"
                  type="email"
                  autoComplete="email"
                  className="w-full rounded-xl border border-slate-300 px-4 py-3"
                  {...formulario.register('email')}
                  aria-invalid={!!formulario.formState.errors.email}
                />
                {formulario.formState.errors.email && (
                  <p role="alert" className="mt-1 text-sm text-red-700">
                    {formulario.formState.errors.email.message}
                  </p>
                )}
              </div>
              {entrar.isError && (
                <p
                  role="alert"
                  className="rounded-lg bg-red-50 p-3 text-sm text-red-800"
                >
                  {entrar.error.message}
                </p>
              )}
              <button
                type="submit"
                disabled={entrar.isPending}
                className="min-h-11 w-full rounded-xl bg-ocean px-5 py-3 font-semibold text-white hover:bg-[#095a6b] disabled:opacity-60"
              >
                {entrar.isPending ? 'Entrando…' : 'Entrar'}
              </button>
            </form>
          ) : (
            <p role="status" className="mt-8 text-slate-600">
              Autenticação indisponível neste ambiente.
            </p>
          )}
        </section>
      </div>
    </main>
  )
}
