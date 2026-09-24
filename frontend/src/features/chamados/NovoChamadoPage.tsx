import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { z } from 'zod'
import {
  criarChamado,
  getCategorias,
  type CriarChamado,
} from '../../api/chamados'
import { Button } from '../../components/ui/button'
import { Card, CardContent } from '../../components/ui/card'
import { Input } from '../../components/ui/input'
import { Textarea } from '../../components/ui/textarea'
import { AreaPage } from '../auth/AreaPage'
import { useMe } from '../auth/useAuth'
import { PessoaPicker } from './PessoaPicker'

const esquema = z.object({
  titulo: z
    .string()
    .trim()
    .min(5, 'Descreva o problema em pelo menos 5 caracteres')
    .max(200),
  categoriaId: z.string().min(1, 'Selecione uma categoria'),
  descricao: z
    .string()
    .trim()
    .min(10, 'Conte um pouco mais sobre o problema')
    .max(10000),
  prioridadeSugerida: z
    .enum(['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'])
    .or(z.literal('')),
  solicitanteId: z.string(),
})

type Campos = z.infer<typeof esquema>

export function NovoChamadoPage() {
  const sessao = useMe()
  const ehTi =
    sessao.data?.perfil === 'TI_AGENTE' || sessao.data?.perfil === 'TI_ADMIN'
  const categorias = useQuery({
    queryKey: ['categorias'],
    queryFn: getCategorias,
    retry: false,
  })
  const cliente = useQueryClient()
  const navegar = useNavigate()
  const formulario = useForm<Campos>({
    resolver: zodResolver(esquema),
    defaultValues: {
      titulo: '',
      categoriaId: '',
      descricao: '',
      prioridadeSugerida: '',
      solicitanteId: '',
    },
  })
  const criar = useMutation({
    mutationFn: criarChamado,
    onSuccess: (chamado) => {
      void cliente.invalidateQueries({ queryKey: ['meus-chamados'] })
      void cliente.invalidateQueries({ queryKey: ['fila-ti'] })
      navegar(`/chamados/${chamado.id}`, { replace: true })
    },
  })

  function enviar(campos: Campos) {
    const dados: CriarChamado = {
      titulo: campos.titulo,
      descricao: campos.descricao,
      categoriaId: Number(campos.categoriaId),
    }
    if (campos.prioridadeSugerida)
      dados.prioridadeSugerida = campos.prioridadeSugerida
    if (ehTi && campos.solicitanteId)
      dados.solicitanteId = Number(campos.solicitanteId)
    criar.mutate(dados)
  }

  return (
    <AreaPage
      titulo="Novo chamado"
      descricao="Conte o que está acontecendo para a equipe de TI começar a ajudar."
    >
      <Card className="mt-8 max-w-2xl">
        <CardContent className="p-6 sm:p-8">
          <form
            onSubmit={formulario.handleSubmit(enviar)}
            className="space-y-6"
            noValidate
          >
            {ehTi && (
              <PessoaPicker
                titulo="Abrir em nome de (opcional)"
                selecionadoId={formulario.watch('solicitanteId')}
                onSelect={(id) => formulario.setValue('solicitanteId', id)}
              />
            )}
            <div>
              <label
                className="mb-2 block text-sm font-semibold"
                htmlFor="titulo"
              >
                Qual é o problema?
              </label>
              <Input
                id="titulo"
                placeholder="Ex.: Não consigo acessar a VPN"
                maxLength={200}
                {...formulario.register('titulo')}
                aria-invalid={!!formulario.formState.errors.titulo}
              />
              {formulario.formState.errors.titulo && (
                <p role="alert" className="mt-1 text-sm text-red-700">
                  {formulario.formState.errors.titulo.message}
                </p>
              )}
            </div>
            <div>
              <label
                className="mb-2 block text-sm font-semibold"
                htmlFor="categoria"
              >
                Categoria
              </label>
              {categorias.isPending ? (
                <p role="status">Carregando categorias…</p>
              ) : categorias.isError ? (
                <div
                  role="alert"
                  className="rounded-lg bg-red-50 p-3 text-sm text-red-800"
                >
                  Não foi possível carregar as categorias.{' '}
                  <Button
                    type="button"
                    variant="link"
                    onClick={() => void categorias.refetch()}
                  >
                    Tentar novamente
                  </Button>
                </div>
              ) : categorias.data.length === 0 ? (
                <p role="status">Nenhuma categoria disponível no momento.</p>
              ) : (
                <select
                  id="categoria"
                  className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm"
                  {...formulario.register('categoriaId')}
                  aria-invalid={!!formulario.formState.errors.categoriaId}
                >
                  <option value="">Selecione uma categoria</option>
                  {categorias.data.map((categoria) => (
                    <option key={categoria.id} value={categoria.id}>
                      {categoria.nome}
                    </option>
                  ))}
                </select>
              )}
              {formulario.formState.errors.categoriaId && (
                <p role="alert" className="mt-1 text-sm text-red-700">
                  {formulario.formState.errors.categoriaId.message}
                </p>
              )}
            </div>
            <div>
              <label
                className="mb-2 block text-sm font-semibold"
                htmlFor="descricao"
              >
                Descreva o que precisa
              </label>
              <Textarea
                id="descricao"
                rows={5}
                placeholder="O que você tentou? Quando começou?"
                maxLength={10000}
                {...formulario.register('descricao')}
                aria-invalid={!!formulario.formState.errors.descricao}
              />
              {formulario.formState.errors.descricao && (
                <p role="alert" className="mt-1 text-sm text-red-700">
                  {formulario.formState.errors.descricao.message}
                </p>
              )}
            </div>
            <div>
              <label
                className="mb-2 block text-sm font-semibold"
                htmlFor="prioridade"
              >
                Urgência sugerida (opcional)
              </label>
              <select
                id="prioridade"
                className="h-10 w-full rounded-md border border-input bg-white px-3 text-sm"
                {...formulario.register('prioridadeSugerida')}
              >
                <option value="">Deixar com a TI</option>
                <option value="BAIXA">Baixa</option>
                <option value="MEDIA">Média</option>
                <option value="ALTA">Alta</option>
                <option value="CRITICA">Crítica</option>
              </select>
              <p className="mt-1 text-xs text-slate-600">
                A prioridade final é definida pela equipe de TI.
              </p>
            </div>
            {criar.isError && (
              <p
                role="alert"
                className="rounded-lg bg-red-50 p-3 text-sm text-red-800"
              >
                {criar.error.message}
              </p>
            )}
            <div className="flex flex-wrap items-center gap-3">
              <Button
                type="submit"
                size="lg"
                disabled={criar.isPending || !categorias.data?.length}
              >
                {criar.isPending ? 'Enviando…' : 'Abrir chamado'}
              </Button>
              <Button variant="outline" asChild>
                <Link to={ehTi ? '/ti/fila' : '/meus-chamados'}>Cancelar</Link>
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </AreaPage>
  )
}
