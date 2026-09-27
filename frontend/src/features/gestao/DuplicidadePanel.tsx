import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { obterDuplicidade, vincularDuplicidade } from '../../api/gestao'
import { Button } from '../../components/ui/button'
import { Card, CardContent } from '../../components/ui/card'

export function DuplicidadePanel({
  chamadoId,
  version,
}: {
  chamadoId: number
  version: number
}) {
  const cliente = useQueryClient()
  const [principal, setPrincipal] = useState('')
  const dados = useQuery({
    queryKey: ['duplicidade', chamadoId],
    queryFn: () => obterDuplicidade(chamadoId),
  })
  const salvar = useMutation({
    mutationFn: (principalId: number | null) =>
      vincularDuplicidade(chamadoId, principalId, version),
    onSuccess: () => {
      setPrincipal('')
      void cliente.invalidateQueries({ queryKey: ['duplicidade', chamadoId] })
      void cliente.invalidateQueries({ queryKey: ['chamado', chamadoId] })
      void cliente.invalidateQueries({ queryKey: ['historico', chamadoId] })
    },
  })
  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const numero = Number(principal)
    if (Number.isSafeInteger(numero) && numero > 0) salvar.mutate(numero)
  }
  return (
    <Card className="mt-6">
      <CardContent className="p-6">
        <h2 className="text-xl font-semibold">Chamados duplicados</h2>
        <p className="mt-1 text-sm text-slate-600">
          Relacione chamados sobre o mesmo problema. O vínculo é visível somente
          à TI.
        </p>
        {dados.isPending && (
          <p role="status" className="mt-4">
            Carregando vínculos…
          </p>
        )}
        {dados.isError && (
          <p role="alert" className="mt-4 text-red-800">
            Falha ao carregar vínculos.{' '}
            <button className="underline" onClick={() => void dados.refetch()}>
              Tentar novamente
            </button>
          </p>
        )}
        {dados.data?.principalId && (
          <p className="mt-4">
            Principal:{' '}
            <Link
              className="font-semibold text-ocean underline"
              to={`/chamados/${dados.data.principalId}`}
            >
              chamado #{dados.data.principalId}
            </Link>
          </p>
        )}
        {dados.data?.duplicados.length ? (
          <ul className="mt-3 list-inside list-disc text-sm">
            {dados.data.duplicados.map((id) => (
              <li key={id}>
                <Link className="text-ocean underline" to={`/chamados/${id}`}>
                  Duplicado #{id}
                </Link>
              </li>
            ))}
          </ul>
        ) : (
          <p className="mt-3 text-sm text-slate-600">
            Nenhum chamado relacionado.
          </p>
        )}
        <form onSubmit={enviar} className="mt-5 flex flex-wrap items-end gap-2">
          <label className="text-sm font-semibold">
            ID do chamado principal
            <input
              type="number"
              min="1"
              value={principal}
              onChange={(e) => setPrincipal(e.target.value)}
              className="mt-1 block h-10 rounded-md border border-slate-300 px-3"
            />
          </label>
          <Button type="submit" disabled={!principal || salvar.isPending}>
            Vincular
          </Button>
          {dados.data?.principalId && (
            <Button
              type="button"
              variant="outline"
              disabled={salvar.isPending}
              onClick={() => salvar.mutate(null)}
            >
              Remover vínculo
            </Button>
          )}
        </form>
        {salvar.isError && (
          <p role="alert" className="mt-3 text-sm text-red-800">
            {salvar.error.message}
          </p>
        )}
      </CardContent>
    </Card>
  )
}
