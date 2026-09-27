import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { avisosAdmin, salvarAviso, type Aviso } from '../../api/gestao'
import { Button } from '../../components/ui/button'
import { Card, CardContent } from '../../components/ui/card'

function local(iso: string): string {
  return iso ? new Date(iso).toISOString().slice(0, 16) : ''
}

export function AvisosAdminPage() {
  const cliente = useQueryClient()
  const [editando, setEditando] = useState<Aviso | null>(null)
  const [titulo, setTitulo] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [inicio, setInicio] = useState(local(new Date().toISOString()))
  const [fim, setFim] = useState('')
  const avisos = useQuery({ queryKey: ['avisos-admin'], queryFn: avisosAdmin })
  const salvar = useMutation({
    mutationFn: (ativo: boolean) =>
      salvarAviso(
        {
          titulo,
          mensagem,
          ativo,
          inicioEm: new Date(inicio).toISOString(),
          fimEm: fim ? new Date(fim).toISOString() : null,
        },
        editando?.id,
      ),
    onSuccess: () => {
      void cliente.invalidateQueries({ queryKey: ['avisos-admin'] })
      void cliente.invalidateQueries({ queryKey: ['avisos'] })
      setEditando(null)
      setTitulo('')
      setMensagem('')
      setFim('')
    },
  })
  function editar(aviso: Aviso) {
    setEditando(aviso)
    setTitulo(aviso.titulo)
    setMensagem(aviso.mensagem)
    setInicio(local(aviso.inicioEm))
    setFim(aviso.fimEm ? local(aviso.fimEm) : '')
  }
  function enviar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    if (
      Number.isFinite(Date.parse(inicio)) &&
      (!fim || Date.parse(fim) > Date.parse(inicio))
    )
      salvar.mutate(editando?.ativo ?? true)
  }
  return (
    <section>
      <h2 className="text-2xl font-semibold">Avisos de incidente</h2>
      <p className="mt-2 text-slate-600">
        Publique um comunicado temporário para todos os usuários do portal.
      </p>
      <Card className="mt-6">
        <CardContent className="p-6">
          <form onSubmit={enviar} className="grid gap-4 sm:grid-cols-2">
            <label className="text-sm font-semibold sm:col-span-2">
              Título
              <input
                required
                maxLength={160}
                value={titulo}
                onChange={(e) => setTitulo(e.target.value)}
                className="mt-1 h-10 w-full rounded-md border p-2"
              />
            </label>
            <label className="text-sm font-semibold sm:col-span-2">
              Mensagem
              <textarea
                required
                maxLength={1000}
                rows={3}
                value={mensagem}
                onChange={(e) => setMensagem(e.target.value)}
                className="mt-1 w-full rounded-md border p-2"
              />
            </label>
            <label className="text-sm font-semibold">
              Início
              <input
                required
                type="datetime-local"
                value={inicio}
                onChange={(e) => setInicio(e.target.value)}
                className="mt-1 h-10 w-full rounded-md border p-2"
              />
            </label>
            <label className="text-sm font-semibold">
              Fim (opcional)
              <input
                type="datetime-local"
                value={fim}
                onChange={(e) => setFim(e.target.value)}
                className="mt-1 h-10 w-full rounded-md border p-2"
              />
            </label>
            {salvar.isError && (
              <p role="alert" className="text-red-800 sm:col-span-2">
                {salvar.error.message}
              </p>
            )}
            <div className="flex flex-wrap gap-2 sm:col-span-2">
              <Button disabled={salvar.isPending} type="submit">
                {editando ? 'Atualizar aviso' : 'Publicar aviso'}
              </Button>
              {editando && (
                <Button
                  variant="outline"
                  type="button"
                  onClick={() => {
                    setEditando(null)
                    setTitulo('')
                    setMensagem('')
                  }}
                >
                  Cancelar
                </Button>
              )}
              {editando?.ativo && (
                <Button
                  variant="outline"
                  type="button"
                  disabled={salvar.isPending}
                  onClick={() => salvar.mutate(false)}
                >
                  Desativar aviso
                </Button>
              )}
            </div>
          </form>
        </CardContent>
      </Card>
      {avisos.isPending && (
        <p role="status" className="mt-6">
          Carregando avisos…
        </p>
      )}
      {avisos.isError && (
        <p role="alert" className="mt-6 text-red-800">
          Não foi possível carregar os avisos.
        </p>
      )}
      {avisos.data?.length === 0 && (
        <p className="mt-6 text-slate-600">Nenhum aviso cadastrado.</p>
      )}
      <ul className="mt-6 space-y-3">
        {avisos.data?.map((aviso) => (
          <li key={aviso.id} className="rounded-xl border bg-white p-4">
            <strong>{aviso.titulo}</strong>
            <span className="ml-2 text-xs">
              {aviso.ativo ? 'Ativo' : 'Desativado'}
            </span>
            <p className="mt-2 whitespace-pre-wrap text-sm">{aviso.mensagem}</p>
            <Button
              variant="outline"
              className="mt-3"
              onClick={() => editar(aviso)}
            >
              Editar
            </Button>
          </li>
        ))}
      </ul>
    </section>
  )
}
