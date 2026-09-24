import { useQuery } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { buscarPessoas, type PessoaBusca } from '../../api/chamados'
import { Button } from '../../components/ui/button'

export function PessoaPicker({
  titulo,
  somenteTi = false,
  selecionadoId,
  onSelect,
}: {
  titulo: string
  somenteTi?: boolean
  selecionadoId: string
  onSelect: (id: string) => void
}) {
  const [texto, setTexto] = useState('')
  const [termo, setTermo] = useState('')
  const [pessoa, setPessoa] = useState<PessoaBusca | null>(null)
  useEffect(() => {
    const temporizador = window.setTimeout(() => setTermo(texto.trim()), 300)
    return () => window.clearTimeout(temporizador)
  }, [texto])
  const pessoas = useQuery({
    queryKey: ['pessoas-busca', termo, somenteTi],
    queryFn: () => buscarPessoas(termo, somenteTi),
    enabled: termo.length >= 2,
    staleTime: 30_000,
    retry: false,
  })

  function escolher(encontrada: PessoaBusca) {
    setPessoa(encontrada)
    onSelect(String(encontrada.id))
    setTexto('')
    setTermo('')
  }

  return (
    <div>
      <label className="block text-sm font-semibold">
        {titulo}
        <input
          type="search"
          value={texto}
          onChange={(evento) => setTexto(evento.target.value)}
          placeholder="Busque por nome ou e-mail"
          maxLength={100}
          className="mt-2 h-10 w-full rounded-md border border-slate-300 px-3"
        />
      </label>
      {selecionadoId && (
        <div className="mt-2 flex flex-wrap items-center gap-2 text-sm">
          <span>
            Selecionado:{' '}
            {pessoa ? `${pessoa.nome} (${pessoa.email})` : `#${selecionadoId}`}
          </span>
          <Button
            type="button"
            variant="link"
            onClick={() => {
              setPessoa(null)
              onSelect('')
            }}
          >
            Limpar seleção
          </Button>
        </div>
      )}
      {texto.trim().length === 1 && (
        <p className="mt-2 text-xs text-slate-600">
          Digite pelo menos duas letras.
        </p>
      )}
      {texto.trim().length >= 2 && termo !== texto.trim() && (
        <p role="status" className="mt-2 text-xs text-slate-600">
          Buscando…
        </p>
      )}
      {termo.length >= 2 && termo === texto.trim() && pessoas.isPending && (
        <p role="status" className="mt-2 text-xs text-slate-600">
          Buscando pessoas…
        </p>
      )}
      {termo.length >= 2 && termo === texto.trim() && pessoas.isError && (
        <p role="alert" className="mt-2 text-xs text-red-700">
          Falha ao buscar pessoas.{' '}
          <button
            type="button"
            className="underline"
            onClick={() => void pessoas.refetch()}
          >
            Tentar novamente
          </button>
        </p>
      )}
      {termo.length >= 2 &&
        termo === texto.trim() &&
        pessoas.data?.length === 0 && (
          <p className="mt-2 text-xs text-slate-600">
            Nenhuma pessoa encontrada.
          </p>
        )}
      {termo.length >= 2 &&
        termo === texto.trim() &&
        pessoas.data &&
        pessoas.data.length > 0 && (
          <ul className="mt-2 max-h-52 overflow-auto rounded-md border border-slate-200 bg-white">
            {pessoas.data.map((encontrada) => (
              <li
                key={encontrada.id}
                className="border-b border-slate-100 last:border-0"
              >
                <button
                  type="button"
                  className="w-full px-3 py-2 text-left text-sm hover:bg-slate-50 focus:bg-slate-50"
                  onClick={() => escolher(encontrada)}
                >
                  <span className="block font-semibold">{encontrada.nome}</span>
                  <span className="block text-xs text-slate-600">
                    {encontrada.email}
                  </span>
                </button>
              </li>
            ))}
          </ul>
        )}
    </div>
  )
}
