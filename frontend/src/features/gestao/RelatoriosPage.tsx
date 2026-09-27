import { useQuery } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { consultarRelatorio, urlCsvRelatorio } from '../../api/gestao'
import { getCategorias } from '../../api/chamados'
import { Button } from '../../components/ui/button'
import { AreaPage } from '../auth/AreaPage'

function dataLocal(data: Date): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(data)
}

export function RelatoriosPage() {
  const hoje = dataLocal(new Date())
  const mesAtras = dataLocal(new Date(Date.now() - 30 * 86400000))
  const [desde, setDesde] = useState(mesAtras)
  const [ate, setAte] = useState(hoje)
  const [setorId, setSetorId] = useState('')
  const [categoriaId, setCategoriaId] = useState('')
  const [aplicado, setAplicado] = useState({
    desde: mesAtras,
    ate: hoje,
    setorId: '',
    categoriaId: '',
  })
  const categorias = useQuery({
    queryKey: ['categorias'],
    queryFn: getCategorias,
  })
  const relatorio = useQuery({
    queryKey: ['relatorio', aplicado],
    queryFn: () =>
      consultarRelatorio(
        aplicado.desde,
        aplicado.ate,
        aplicado.setorId ? Number(aplicado.setorId) : undefined,
        aplicado.categoriaId ? Number(aplicado.categoriaId) : undefined,
      ),
    retry: false,
  })
  function aplicar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setAplicado({ desde, ate, setorId, categoriaId })
  }
  return (
    <AreaPage
      titulo="Relatórios"
      descricao="Compare volumes e resoluções por setor, categoria e período."
    >
      <form
        onSubmit={aplicar}
        className="mt-8 grid gap-4 rounded-xl border bg-white p-5 sm:grid-cols-2 lg:grid-cols-4"
      >
        <label className="text-sm font-semibold">
          De
          <input
            required
            type="date"
            value={desde}
            onChange={(e) => setDesde(e.target.value)}
            className="mt-1 h-10 w-full rounded-md border p-2"
          />
        </label>
        <label className="text-sm font-semibold">
          Até
          <input
            required
            type="date"
            value={ate}
            onChange={(e) => setAte(e.target.value)}
            className="mt-1 h-10 w-full rounded-md border p-2"
          />
        </label>
        <label className="text-sm font-semibold">
          Setor (ID opcional)
          <input
            type="number"
            min="1"
            value={setorId}
            onChange={(e) => setSetorId(e.target.value)}
            className="mt-1 h-10 w-full rounded-md border p-2"
          />
        </label>
        <label className="text-sm font-semibold">
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
        <div className="flex flex-wrap items-center gap-3 sm:col-span-2 lg:col-span-4">
          <Button type="submit">Aplicar</Button>
          {relatorio.data && (
            <a
              className="font-semibold text-ocean underline"
              href={urlCsvRelatorio(
                aplicado.desde,
                aplicado.ate,
                aplicado.setorId ? Number(aplicado.setorId) : undefined,
                aplicado.categoriaId ? Number(aplicado.categoriaId) : undefined,
              )}
            >
              Exportar CSV
            </a>
          )}
        </div>
      </form>
      {relatorio.isPending && (
        <p role="status" className="mt-6">
          Calculando relatório…
        </p>
      )}
      {relatorio.isError && (
        <p role="alert" className="mt-6 text-red-800">
          {relatorio.error.message}{' '}
          <button
            className="underline"
            onClick={() => void relatorio.refetch()}
          >
            Tentar novamente
          </button>
        </p>
      )}
      {relatorio.data?.linhas.length === 0 && (
        <p className="mt-6 text-slate-600">Nenhum chamado no período.</p>
      )}
      {relatorio.data && relatorio.data.linhas.length > 0 && (
        <div className="mt-6 overflow-x-auto rounded-xl border bg-white">
          <table className="w-full min-w-[600px] text-left text-sm">
            <thead className="bg-slate-50">
              <tr>
                <th className="p-3">Setor</th>
                <th className="p-3">Categoria</th>
                <th className="p-3">Total</th>
                <th className="p-3">Resolvidos</th>
                <th className="p-3">Média de resolução</th>
              </tr>
            </thead>
            <tbody>
              {relatorio.data.linhas.map((linha, indice) => (
                <tr key={indice} className="border-t">
                  <td className="p-3">{linha.setor}</td>
                  <td className="p-3">{linha.categoria}</td>
                  <td className="p-3">{linha.total}</td>
                  <td className="p-3">{linha.resolvidos}</td>
                  <td className="p-3">
                    {linha.mediaResolucaoHoras == null
                      ? '—'
                      : `${linha.mediaResolucaoHoras.toFixed(1)} h`}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </AreaPage>
  )
}
