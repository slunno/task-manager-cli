import { Link, Outlet } from 'react-router'
import { AreaPage } from '../auth/AreaPage'

const paginas = [
  ['Usuários', '/ti/admin/usuarios'],
  ['Categorias', '/ti/admin/categorias'],
  ['Políticas de SLA', '/ti/admin/slas'],
  ['Calendário', '/ti/admin/calendario'],
  ['Avisos de incidente', '/ti/admin/avisos'],
]

export function AdminLayout() {
  return (
    <AreaPage
      titulo="Administração"
      descricao="Configurações do atendimento e acesso à plataforma."
    >
      <nav aria-label="Administração" className="mt-8 flex flex-wrap gap-3">
        {paginas.map(([nome, destino]) => (
          <Link
            key={destino}
            to={destino}
            className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-ocean hover:bg-mist"
          >
            {nome}
          </Link>
        ))}
      </nav>
      <Outlet />
    </AreaPage>
  )
}
