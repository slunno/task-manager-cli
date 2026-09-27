import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import {
  buscarArtigos,
  salvarArtigo,
  salvarResposta,
  listarRespostas,
  type Artigo,
  type RespostaPronta,
} from '../../api/conhecimento'
import { getCategorias } from '../../api/chamados'
import { Button } from '../../components/ui/button'
import { Card, CardContent } from '../../components/ui/card'
import { AreaPage } from '../auth/AreaPage'
import { useMe } from '../auth/useAuth'

export function ConhecimentoPage() {
  const usuario = useMe().data
  const ti = usuario?.perfil !== 'FUNCIONARIO'
  const cliente = useQueryClient()
  const [busca, setBusca] = useState('')
  const [texto, setTexto] = useState('')
  const [pagina, setPagina] = useState(0)
  const [selecionado, setSelecionado] = useState<Artigo | null>(null)
  const [titulo, setTitulo] = useState('')
  const [conteudo, setConteudo] = useState('')
  const [categoriaId, setCategoriaId] = useState('')
  const [publicado, setPublicado] = useState(false)
  const [tituloResposta, setTituloResposta] = useState('')
  const [textoResposta, setTextoResposta] = useState('')
  const [respostaEditada, setRespostaEditada] = useState<RespostaPronta | null>(
    null,
  )
  const artigos = useQuery({
    queryKey: ['artigos', busca, pagina],
    queryFn: () => buscarArtigos(busca, pagina),
    retry: false,
  })
  const categorias = useQuery({
    queryKey: ['categorias'],
    queryFn: getCategorias,
  })
  const respostas = useQuery({
    queryKey: ['respostas-prontas'],
    queryFn: listarRespostas,
    enabled: ti,
  })
  const salvar = useMutation({
    mutationFn: () =>
      salvarArtigo(
        {
          titulo,
          conteudo,
          categoriaId: categoriaId ? Number(categoriaId) : null,
          publicado,
        },
        selecionado?.id,
      ),
    onSuccess: () => {
      void cliente.invalidateQueries({ queryKey: ['artigos'] })
      setSelecionado(null)
      setTitulo('')
      setConteudo('')
      setCategoriaId('')
      setPublicado(false)
    },
  })
  const salvarModelo = useMutation({
    mutationFn: () =>
      salvarResposta(
        { titulo: tituloResposta, texto: textoResposta, ativo: true },
        respostaEditada?.id,
      ),
    onSuccess: () => {
      void cliente.invalidateQueries({ queryKey: ['respostas-prontas'] })
      setTituloResposta('')
      setTextoResposta('')
      setRespostaEditada(null)
    },
  })

  function pesquisar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setPagina(0)
    setBusca(texto.trim())
  }
  function editar(artigo: Artigo) {
    setSelecionado(artigo)
    setTitulo(artigo.titulo)
    setConteudo(artigo.conteudo)
    setCategoriaId(artigo.categoriaId ? String(artigo.categoriaId) : '')
    setPublicado(artigo.publicado)
  }

  return (
    <AreaPage
      titulo="Base de conhecimento"
      descricao="Encontre orientações antes de abrir um chamado."
    >
      <form onSubmit={pesquisar} className="mt-8 flex flex-wrap gap-2">
        <label className="min-w-60 flex-1 text-sm font-semibold">
          Buscar artigos
          <input
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            maxLength={120}
            className="mt-2 h-10 w-full rounded-md border border-slate-300 px-3"
            placeholder="Ex.: acesso à VPN"
          />
        </label>
        <Button className="self-end" type="submit">
          Buscar
        </Button>
      </form>
      {artigos.isPending && (
        <p role="status" className="mt-6">
          Buscando artigos…
        </p>
      )}
      {artigos.isError && (
        <p role="alert" className="mt-6 text-red-800">
          Falha ao buscar artigos.{' '}
          <button className="underline" onClick={() => void artigos.refetch()}>
            Tentar novamente
          </button>
        </p>
      )}
      {artigos.data?.content.length === 0 && (
        <p className="mt-6 text-slate-600">Nenhum artigo encontrado.</p>
      )}
      <div className="mt-6 space-y-3">
        {artigos.data?.content.map((artigo) => (
          <Card key={artigo.id}>
            <CardContent className="p-5">
              <h2 className="text-lg font-semibold">{artigo.titulo}</h2>
              {!artigo.publicado && (
                <span className="text-sm text-amber-800">
                  Rascunho · visível somente à TI
                </span>
              )}
              <p className="mt-3 whitespace-pre-wrap text-sm leading-6">
                {artigo.conteudo}
              </p>
              {ti && (
                <Button
                  variant="outline"
                  className="mt-4"
                  onClick={() => editar(artigo)}
                >
                  Editar
                </Button>
              )}
            </CardContent>
          </Card>
        ))}
      </div>
      {artigos.data && artigos.data.totalPages > 1 && (
        <div className="mt-5 flex items-center gap-3">
          <Button
            variant="outline"
            disabled={pagina === 0}
            onClick={() => setPagina(pagina - 1)}
          >
            Anterior
          </Button>
          <span>
            Página {pagina + 1} de {artigos.data.totalPages}
          </span>
          <Button
            variant="outline"
            disabled={pagina + 1 >= artigos.data.totalPages}
            onClick={() => setPagina(pagina + 1)}
          >
            Próxima
          </Button>
        </div>
      )}
      {ti && (
        <section className="mt-12 grid gap-6 lg:grid-cols-2">
          <Card>
            <CardContent className="space-y-4 p-6">
              <h2 className="text-xl font-semibold">
                {selecionado ? 'Editar artigo' : 'Novo artigo'}
              </h2>
              <form
                className="space-y-4"
                onSubmit={(e) => {
                  e.preventDefault()
                  salvar.mutate()
                }}
              >
                <label className="block text-sm font-semibold">
                  Título
                  <input
                    required
                    minLength={5}
                    maxLength={200}
                    value={titulo}
                    onChange={(e) => setTitulo(e.target.value)}
                    className="mt-1 h-10 w-full rounded-md border p-2"
                  />
                </label>
                <label className="block text-sm font-semibold">
                  Conteúdo
                  <textarea
                    required
                    minLength={20}
                    maxLength={20000}
                    rows={7}
                    value={conteudo}
                    onChange={(e) => setConteudo(e.target.value)}
                    className="mt-1 w-full rounded-md border p-2"
                  />
                </label>
                <label className="block text-sm font-semibold">
                  Categoria
                  <select
                    value={categoriaId}
                    onChange={(e) => setCategoriaId(e.target.value)}
                    className="mt-1 h-10 w-full rounded-md border p-2"
                  >
                    <option value="">Todas</option>
                    {categorias.data?.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.nome}
                      </option>
                    ))}
                  </select>
                </label>
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={publicado}
                    onChange={(e) => setPublicado(e.target.checked)}
                  />{' '}
                  Publicar para funcionários
                </label>
                {salvar.isError && (
                  <p role="alert" className="text-red-800">
                    {salvar.error.message}
                  </p>
                )}
                <Button disabled={salvar.isPending} type="submit">
                  {salvar.isPending ? 'Salvando…' : 'Salvar artigo'}
                </Button>
                {selecionado && (
                  <Button
                    type="button"
                    variant="outline"
                    className="ml-2"
                    onClick={() => {
                      setSelecionado(null)
                      setTitulo('')
                      setConteudo('')
                      setPublicado(false)
                    }}
                  >
                    Cancelar edição
                  </Button>
                )}
              </form>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="space-y-4 p-6">
              <h2 className="text-xl font-semibold">Respostas prontas</h2>
              {respostas.isPending && (
                <p role="status">Carregando respostas…</p>
              )}
              {respostas.isError && (
                <p role="alert">Não foi possível carregar respostas.</p>
              )}
              {respostas.data?.length === 0 && (
                <p className="text-sm text-slate-600">
                  Nenhuma resposta cadastrada.
                </p>
              )}
              <ul className="space-y-2">
                {respostas.data?.map((r) => (
                  <li key={r.id}>
                    <button
                      className="font-semibold text-ocean underline"
                      onClick={() => {
                        setRespostaEditada(r)
                        setTituloResposta(r.titulo)
                        setTextoResposta(r.texto)
                      }}
                    >
                      {r.titulo}
                    </button>
                  </li>
                ))}
              </ul>
              <form
                className="space-y-3 border-t pt-4"
                onSubmit={(e) => {
                  e.preventDefault()
                  salvarModelo.mutate()
                }}
              >
                <label className="block text-sm font-semibold">
                  Nome
                  <input
                    required
                    maxLength={120}
                    value={tituloResposta}
                    onChange={(e) => setTituloResposta(e.target.value)}
                    className="mt-1 h-10 w-full rounded-md border p-2"
                  />
                </label>
                <label className="block text-sm font-semibold">
                  Texto
                  <textarea
                    required
                    maxLength={10000}
                    rows={5}
                    value={textoResposta}
                    onChange={(e) => setTextoResposta(e.target.value)}
                    className="mt-1 w-full rounded-md border p-2"
                  />
                </label>
                {salvarModelo.isError && (
                  <p role="alert" className="text-red-800">
                    {salvarModelo.error.message}
                  </p>
                )}
                <Button disabled={salvarModelo.isPending} type="submit">
                  Salvar resposta
                </Button>
              </form>
            </CardContent>
          </Card>
        </section>
      )}
    </AreaPage>
  )
}
