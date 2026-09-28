import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { z } from 'zod'
import { destinoInicial, loginPassword } from '../../api/auth'
import { ME_QUERY_KEY } from './useAuth'

const schema = z.object({
  email: z.email('Informe um e-mail válido'),
  senha: z.string().min(1, 'Informe sua senha').max(256),
})
type Campos = z.infer<typeof schema>

export function PasswordLoginForm() {
  const client = useQueryClient()
  const navigate = useNavigate()
  const form = useForm<Campos>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', senha: '' },
  })
  const login = useMutation({
    mutationFn: loginPassword,
    onSuccess: (user) => {
      form.reset()
      client.setQueryData(ME_QUERY_KEY, user)
      navigate(destinoInicial(user.perfil), { replace: true })
    },
    onSettled: () => {
      form.setValue('senha', '')
    },
  })

  return (
    <form
      className="mt-7 space-y-5"
      onSubmit={form.handleSubmit((data) => login.mutate(data))}
      noValidate
    >
      <div>
        <label htmlFor="email" className="mb-2 block text-sm font-medium">
          E-mail
        </label>
        <input
          id="email"
          type="email"
          autoComplete="username"
          className="w-full rounded-xl border border-slate-300 px-4 py-3"
          {...form.register('email')}
          aria-invalid={!!form.formState.errors.email}
        />
        {form.formState.errors.email && (
          <p role="alert" className="mt-1 text-sm text-red-700">
            {form.formState.errors.email.message}
          </p>
        )}
      </div>
      <div>
        <label htmlFor="senha" className="mb-2 block text-sm font-medium">
          Senha
        </label>
        <input
          id="senha"
          type="password"
          autoComplete="current-password"
          className="w-full rounded-xl border border-slate-300 px-4 py-3"
          {...form.register('senha')}
          aria-invalid={!!form.formState.errors.senha}
        />
        {form.formState.errors.senha && (
          <p role="alert" className="mt-1 text-sm text-red-700">
            {form.formState.errors.senha.message}
          </p>
        )}
      </div>
      {login.isError && (
        <p
          role="alert"
          className="rounded-lg bg-red-50 p-3 text-sm text-red-800"
        >
          {login.error.message}
        </p>
      )}
      <button
        type="submit"
        disabled={login.isPending}
        className="min-h-11 w-full rounded-xl bg-ocean px-5 py-3 font-semibold text-white hover:bg-[#095a6b] disabled:opacity-60"
      >
        {login.isPending ? 'Entrando…' : 'Entrar'}
      </button>
    </form>
  )
}
