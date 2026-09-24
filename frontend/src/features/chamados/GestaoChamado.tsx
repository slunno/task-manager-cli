import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import {
  assumirChamado,
  atualizarChamado,
  type AtualizarChamado,
  type Categoria,
  type Chamado,
  type StatusChamado,
} from '../../api/chamados'
import { Button } from '../../components/ui/button'
import { Card, CardContent, CardHeader } from '../../components/ui/card'
import { prioridadeTexto, statusTexto } from './formatacao'
import { PessoaPicker } from './PessoaPicker'

const esquema = z.object({
  prioridade: z.enum(['BAIXA', 'MEDIA', 'ALTA', 'CRITICA']),
  categoriaId: z.string().min(1),
  responsavelId: z.string(),
  removerResponsavel: z.boolean(),
  status: z
    .enum(['EM_ATENDIMENTO', 'AGUARDANDO_USUARIO', 'RESOLVIDO'])
    .or(z.literal('')),
  solucao: z.string().max(10000),
})
type Campos = z.infer<typeof esquema>

const transicoes: Record<StatusChamado, StatusChamado[]> = {
  ABERTO: [],
  EM_ATENDIMENTO: ['AGUARDANDO_USUARIO', 'RESOLVIDO'],
  AGUARDANDO_USUARIO: ['EM_ATENDIMENTO'],
  RESOLVIDO: [],
  FECHADO: [],
}

export function GestaoChamado({
  chamado,
  categorias,
}: {
  chamado: Chamado
  categorias: Categoria[]
}) {
  const cliente = useQueryClient()
  const [erroLocal, setErroLocal] = useState<string | null>(null)
  const formulario = useForm<Campos>({
    resolver: zodResolver(esquema),
    defaultValues: {
      prioridade: chamado.prioridade,
      categoriaId: String(chamado.categoriaId),
      responsavelId: '',
      removerResponsavel: false,
      status: '',
      solucao: '',
    },
  })
  const statusSelecionado = formulario.watch('status')

  function atualizou(novo: Chamado) {
    cliente.setQueryData(['chamado', chamado.id], novo)
    void cliente.invalidateQueries({ queryKey: ['historico', chamado.id] })
    void cliente.invalidateQueries({ queryKey: ['fila-ti'] })
    void cliente.invalidateQueries({ queryKey: ['meus-chamados'] })
  }

  const assumir = useMutation({
    mutationFn: () => assumirChamado(chamado.id, chamado.version),
    onSuccess: atualizou,
  })
  const atualizar = useMutation({
    mutationFn: (dados: AtualizarChamado) =>
      atualizarChamado(chamado.id, dados),
    onSuccess: atualizou,
  })

  function enviar(campos: Campos) {
    setErroLocal(null)
    const dados: AtualizarChamado = { version: chamado.version }
    if (campos.prioridade !== chamado.prioridade)
      dados.prioridade = campos.prioridade
    if (Number(campos.categoriaId) !== chamado.categoriaId)
      dados.categoriaId = Number(campos.categoriaId)
    const responsavel = campos.responsavelId.trim()
    if (responsavel && campos.removerResponsavel) {
      setErroLocal('Selecione um responsável ou retire a atribuição.')
      return
    }
    if (responsavel && Number(responsavel) !== chamado.responsavelId) {
      const numero = Number(responsavel)
      if (!Number.isSafeInteger(numero) || numero <= 0) {
        setErroLocal('Informe um ID de responsável válido.')
        return
      }
      dados.responsavelId = numero
    } else if (campos.removerResponsavel && chamado.responsavelId) {
      dados.removerResponsavel = true
    }
    if (campos.status) {
      dados.status = campos.status
      if (campos.status === 'RESOLVIDO') {
        if (!campos.solucao.trim()) {
          setErroLocal('Informe a solução antes de resolver o chamado.')
          return
        }
        dados.solucao = campos.solucao.trim()
      }
    }
    if (Object.keys(dados).length === 1) {
      setErroLocal('Altere ao menos um campo antes de salvar.')
      return
    }
    atualizar.mutate(dados)
  }

  const podeAssumir =
    !chamado.responsavelId &&
    chamado.status !== 'RESOLVIDO' &&
    chamado.status !== 'FECHADO'

  return (
    <Card className="mt-6">
      <CardHeader>
        <h2 className="text-xl font-semibold">Operação da TI</h2>
        <p className="text-sm text-slate-600">
          Alterações ficam registradas no histórico do chamado.
        </p>
      </CardHeader>
      <CardContent>
        {podeAssumir && (
          <div className="mb-6">
            <Button
              disabled={assumir.isPending || atualizar.isPending}
              onClick={() => assumir.mutate()}
            >
              {assumir.isPending ? 'Assumindo…' : 'Assumir chamado'}
            </Button>
          </div>
        )}
        <form
          onSubmit={formulario.handleSubmit(enviar)}
          className="grid gap-5 sm:grid-cols-2"
        >
          <label className="text-sm font-semibold">
            Prioridade
            <select
              {...formulario.register('prioridade')}
              className="mt-2 h-10 w-full rounded-md border border-slate-300 bg-white px-3"
            >
              {(['BAIXA', 'MEDIA', 'ALTA', 'CRITICA'] as const).map((valor) => (
                <option key={valor} value={valor}>
                  {prioridadeTexto[valor]}
                </option>
              ))}
            </select>
          </label>
          <label className="text-sm font-semibold">
            Categoria
            <select
              {...formulario.register('categoriaId')}
              className="mt-2 h-10 w-full rounded-md border border-slate-300 bg-white px-3"
            >
              {categorias.map((categoria) => (
                <option key={categoria.id} value={categoria.id}>
                  {categoria.nome}
                </option>
              ))}
            </select>
          </label>
          <div>
            <p className="mb-2 text-sm text-slate-600">
              Responsável atual:{' '}
              {chamado.responsavelId ? `#${chamado.responsavelId}` : 'nenhum'}
            </p>
            <PessoaPicker
              titulo="Atribuir a alguém da TI"
              somenteTi
              selecionadoId={formulario.watch('responsavelId')}
              onSelect={(id) => formulario.setValue('responsavelId', id)}
            />
            {chamado.responsavelId && (
              <label className="mt-3 flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  {...formulario.register('removerResponsavel')}
                />
                Retirar atribuição
              </label>
            )}
          </div>
          <label className="text-sm font-semibold">
            Próximo status
            <select
              {...formulario.register('status')}
              className="mt-2 h-10 w-full rounded-md border border-slate-300 bg-white px-3"
            >
              <option value="">Manter status atual</option>
              {transicoes[chamado.status].map((valor) => (
                <option key={valor} value={valor}>
                  {statusTexto[valor]}
                </option>
              ))}
            </select>
          </label>
          {statusSelecionado === 'RESOLVIDO' && (
            <label className="text-sm font-semibold sm:col-span-2">
              Solução aplicada
              <textarea
                rows={4}
                maxLength={10000}
                {...formulario.register('solucao')}
                className="mt-2 w-full rounded-md border border-slate-300 p-3"
              />
            </label>
          )}
          {(erroLocal || assumir.isError || atualizar.isError) && (
            <div
              role="alert"
              className="rounded-lg bg-red-50 p-3 text-sm text-red-800 sm:col-span-2"
            >
              {erroLocal ?? assumir.error?.message ?? atualizar.error?.message}
              {(assumir.isError || atualizar.isError) && (
                <button
                  type="button"
                  className="ml-2 font-semibold underline"
                  onClick={() =>
                    void cliente.invalidateQueries({
                      queryKey: ['chamado', chamado.id],
                    })
                  }
                >
                  Atualizar chamado
                </button>
              )}
            </div>
          )}
          <div className="sm:col-span-2">
            <Button
              type="submit"
              disabled={
                atualizar.isPending ||
                assumir.isPending ||
                chamado.status === 'FECHADO'
              }
            >
              {atualizar.isPending ? 'Salvando…' : 'Salvar alterações'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
