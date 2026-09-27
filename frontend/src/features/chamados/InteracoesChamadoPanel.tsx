import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import {
  criarComentario,
  enviarAnexo,
  getAnexos,
  getComentarios,
} from '../../api/interacoes'
import { Button } from '../../components/ui/button'
import { Card, CardContent, CardHeader } from '../../components/ui/card'
import { dataHora } from '../../lib/dataHora'
import { LinhaTempoPublica } from './LinhaTempoPublica'
import { listarRespostas } from '../../api/conhecimento'

const tiposPermitidos = 'application/pdf,image/png,image/jpeg,text/plain'
const limite = 10 * 1024 * 1024

export function InteracoesChamadoPanel({
  chamadoId,
  usuarioId,
  ehTi,
  concluido,
}: {
  chamadoId: number
  usuarioId: number
  ehTi: boolean
  concluido: boolean
}) {
  const queryClient = useQueryClient()
  const [paginaComentarios, setPaginaComentarios] = useState(0)
  const [paginaAnexos, setPaginaAnexos] = useState(0)
  const [texto, setTexto] = useState('')
  const [interno, setInterno] = useState(false)
  const [anexoInterno, setAnexoInterno] = useState(false)
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [erroArquivo, setErroArquivo] = useState('')
  const comentarios = useQuery({
    queryKey: ['comentarios', chamadoId, paginaComentarios],
    queryFn: () => getComentarios(chamadoId, paginaComentarios),
    retry: false,
    enabled: ehTi,
  })
  const respostas = useQuery({
    queryKey: ['respostas-prontas'],
    queryFn: listarRespostas,
    enabled: ehTi && !concluido,
  })
  const anexos = useQuery({
    queryKey: ['anexos', chamadoId, paginaAnexos],
    queryFn: () => getAnexos(chamadoId, paginaAnexos),
    retry: false,
  })
  const salvarComentario = useMutation({
    mutationFn: () => criarComentario(chamadoId, texto, interno),
    onSuccess: async () => {
      setTexto('')
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['comentarios', chamadoId] }),
        queryClient.invalidateQueries({ queryKey: ['linha-tempo', chamadoId] }),
        queryClient.invalidateQueries({ queryKey: ['chamado', chamadoId] }),
        queryClient.invalidateQueries({ queryKey: ['historico', chamadoId] }),
      ])
    },
  })
  const salvarAnexo = useMutation({
    mutationFn: () => enviarAnexo(chamadoId, arquivo as File, anexoInterno),
    onSuccess: async () => {
      setArquivo(null)
      const campo = document.getElementById(
        'arquivo-chamado',
      ) as HTMLInputElement | null
      if (campo) campo.value = ''
      await queryClient.invalidateQueries({ queryKey: ['anexos', chamadoId] })
    },
  })

  function comentar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    if (texto.trim() && !concluido) salvarComentario.mutate()
  }

  function anexar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    if (!arquivo || concluido) return
    if (arquivo.size === 0 || arquivo.size > limite) {
      setErroArquivo('Escolha um arquivo de até 10 MB.')
      return
    }
    if (!tiposPermitidos.split(',').includes(arquivo.type)) {
      setErroArquivo('Use PDF, PNG, JPG ou texto simples.')
      return
    }
    setErroArquivo('')
    salvarAnexo.mutate()
  }

  return (
    <div className="mt-6 grid gap-5 lg:grid-cols-2">
      <Card>
        <CardHeader>
          <h2 className="text-xl font-semibold">Conversa do chamado</h2>
          <p className="text-sm text-slate-600">
            Mensagens públicas são compartilhadas com o solicitante.
          </p>
        </CardHeader>
        <CardContent>
          {!ehTi && (
            <LinhaTempoPublica chamadoId={chamadoId} usuarioId={usuarioId} />
          )}
          {ehTi && (
            <>
              {comentarios.isPending && (
                <p role="status">Carregando mensagens…</p>
              )}
              {comentarios.isError && (
                <p role="alert" className="text-red-800">
                  Não foi possível carregar as mensagens.{' '}
                  <button
                    className="underline"
                    onClick={() => void comentarios.refetch()}
                  >
                    Tentar novamente
                  </button>
                </p>
              )}
              {comentarios.data?.content.length === 0 && (
                <p className="text-sm text-slate-600">
                  Ainda não há mensagens.
                </p>
              )}
              {comentarios.data && comentarios.data.content.length > 0 && (
                <ol className="space-y-4">
                  {comentarios.data.content.map((item) => (
                    <li
                      key={item.id}
                      className={`rounded-xl p-4 ${item.interno ? 'border border-amber-300 bg-amber-50' : 'bg-slate-50'}`}
                    >
                      <div className="flex flex-wrap items-center gap-2 text-sm font-semibold">
                        <span>
                          {item.autorId === usuarioId
                            ? 'Você'
                            : ehTi
                              ? `Usuário #${item.autorId}`
                              : 'Equipe de TI'}
                        </span>
                        {item.interno && (
                          <span className="rounded-full bg-amber-200 px-2 py-0.5 text-xs text-amber-900">
                            Nota interna · apenas TI
                          </span>
                        )}
                      </div>
                      <p className="mt-2 whitespace-pre-wrap break-words">
                        {item.texto}
                      </p>
                      <time
                        className="mt-2 block text-xs text-slate-500"
                        dateTime={item.criadoEm}
                      >
                        {dataHora(item.criadoEm)}
                      </time>
                    </li>
                  ))}
                </ol>
              )}
              {comentarios.data && comentarios.data.totalPages > 1 && (
                <div className="mt-5 flex items-center justify-between text-sm">
                  <Button
                    variant="outline"
                    disabled={paginaComentarios === 0}
                    onClick={() => setPaginaComentarios(paginaComentarios - 1)}
                  >
                    Anterior
                  </Button>
                  <span>
                    Página {paginaComentarios + 1} de{' '}
                    {comentarios.data.totalPages}
                  </span>
                  <Button
                    variant="outline"
                    disabled={
                      paginaComentarios + 1 >= comentarios.data.totalPages
                    }
                    onClick={() => setPaginaComentarios(paginaComentarios + 1)}
                  >
                    Próxima
                  </Button>
                </div>
              )}
            </>
          )}
          {!concluido && (
            <form className="mt-6 space-y-3 border-t pt-5" onSubmit={comentar}>
              {ehTi && (
                <label className="block text-sm font-semibold">
                  Usar resposta pronta
                  <select
                    className="mt-2 h-10 w-full rounded-md border border-slate-300 bg-white px-3"
                    defaultValue=""
                    onChange={(e) => {
                      const modelo = respostas.data?.find(
                        (r) => r.id === Number(e.target.value),
                      )
                      if (modelo) setTexto(modelo.texto)
                    }}
                  >
                    <option value="">Selecione, se desejar</option>
                    {respostas.data?.map((r) => (
                      <option key={r.id} value={r.id}>
                        {r.titulo}
                      </option>
                    ))}
                  </select>
                  {respostas.isError && (
                    <span className="mt-1 block text-red-800">
                      Respostas prontas indisponíveis.
                    </span>
                  )}
                </label>
              )}
              <label
                htmlFor="texto-comentario"
                className="block text-sm font-semibold"
              >
                {interno ? 'Nova nota interna' : 'Nova mensagem'}
              </label>
              <textarea
                id="texto-comentario"
                value={texto}
                maxLength={10000}
                required
                onChange={(evento) => setTexto(evento.target.value)}
                className="min-h-28 w-full rounded-lg border border-slate-300 p-3"
                placeholder="Escreva sua mensagem"
              />
              {ehTi && (
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={interno}
                    onChange={(evento) => setInterno(evento.target.checked)}
                  />
                  Nota interna (visível somente à TI)
                </label>
              )}
              {salvarComentario.isError && (
                <p role="alert" className="text-sm text-red-800">
                  Não foi possível enviar. {salvarComentario.error.message}
                </p>
              )}
              <Button
                type="submit"
                disabled={salvarComentario.isPending || !texto.trim()}
              >
                {salvarComentario.isPending ? 'Enviando…' : 'Enviar mensagem'}
              </Button>
            </form>
          )}
          {concluido && (
            <p className="mt-5 text-sm text-slate-600">
              Este chamado está concluído e não aceita novas mensagens.
            </p>
          )}
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <h2 className="text-xl font-semibold">Anexos</h2>
          <p className="text-sm text-slate-600">
            PDF, PNG, JPG ou texto simples · até 10 MB.
          </p>
        </CardHeader>
        <CardContent>
          {anexos.isPending && <p role="status">Carregando anexos…</p>}
          {anexos.isError && (
            <p role="alert" className="text-red-800">
              Não foi possível carregar os anexos.{' '}
              <button
                className="underline"
                onClick={() => void anexos.refetch()}
              >
                Tentar novamente
              </button>
            </p>
          )}
          {anexos.data?.content.length === 0 && (
            <p className="text-sm text-slate-600">Nenhum anexo enviado.</p>
          )}
          {anexos.data && anexos.data.content.length > 0 && (
            <ul className="space-y-3">
              {anexos.data.content.map((item) => (
                <li
                  key={item.id}
                  className="rounded-lg border border-slate-200 p-3"
                >
                  <a
                    className="break-all font-semibold text-ocean underline"
                    href={`/api/v1/anexos/${item.id}/download`}
                  >
                    {item.nomeOriginal}
                  </a>
                  {item.interno && (
                    <span className="ml-2 text-xs font-semibold text-amber-800">
                      Interno · apenas TI
                    </span>
                  )}
                  <p className="mt-1 text-xs text-slate-500">
                    {(item.tamanho / 1024).toFixed(1)} KB ·{' '}
                    {dataHora(item.criadoEm)}
                  </p>
                </li>
              ))}
            </ul>
          )}
          {anexos.data && anexos.data.totalPages > 1 && (
            <div className="mt-5 flex items-center justify-between text-sm">
              <Button
                variant="outline"
                disabled={paginaAnexos === 0}
                onClick={() => setPaginaAnexos(paginaAnexos - 1)}
              >
                Anterior
              </Button>
              <span>
                Página {paginaAnexos + 1} de {anexos.data.totalPages}
              </span>
              <Button
                variant="outline"
                disabled={paginaAnexos + 1 >= anexos.data.totalPages}
                onClick={() => setPaginaAnexos(paginaAnexos + 1)}
              >
                Próxima
              </Button>
            </div>
          )}
          {!concluido && (
            <form className="mt-6 space-y-3 border-t pt-5" onSubmit={anexar}>
              <label
                htmlFor="arquivo-chamado"
                className="block text-sm font-semibold"
              >
                Adicionar arquivo
              </label>
              <input
                id="arquivo-chamado"
                type="file"
                accept={tiposPermitidos}
                onChange={(evento) => {
                  setArquivo(evento.target.files?.[0] ?? null)
                  setErroArquivo('')
                }}
                className="block w-full text-sm"
              />
              {ehTi && (
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={anexoInterno}
                    onChange={(evento) =>
                      setAnexoInterno(evento.target.checked)
                    }
                  />
                  Anexo interno (visível somente à TI)
                </label>
              )}
              {erroArquivo && (
                <p role="alert" className="text-sm text-red-800">
                  {erroArquivo}
                </p>
              )}
              {salvarAnexo.isError && (
                <p role="alert" className="text-sm text-red-800">
                  Falha no envio. {salvarAnexo.error.message}
                </p>
              )}
              <Button
                type="submit"
                disabled={!arquivo || salvarAnexo.isPending}
              >
                {salvarAnexo.isPending ? 'Enviando…' : 'Enviar anexo'}
              </Button>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
