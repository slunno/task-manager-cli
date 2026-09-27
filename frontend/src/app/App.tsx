import { AdminLayout } from '../features/admin/AdminLayout'
import { UsuariosAdminPage } from '../features/admin/UsuariosAdminPage'
import { CategoriasAdminPage } from '../features/admin/CategoriasAdminPage'
import { SlasAdminPage } from '../features/admin/SlasAdminPage'
import { CalendarioAdminPage } from '../features/admin/CalendarioAdminPage'
import { Link, Route, Routes } from 'react-router'
import { Entrada, ExigirPerfil } from '../features/auth/Guardas'
import { LoginPage } from '../features/auth/LoginPage'
import { DetalheChamadoPage } from '../features/chamados/DetalheChamadoPage'
import { FilaTiPage } from '../features/chamados/FilaTiPage'
import { DashboardTiPage } from '../features/chamados/DashboardTiPage'
import { MeusChamadosPage } from '../features/chamados/MeusChamadosPage'
import { NovoChamadoPage } from '../features/chamados/NovoChamadoPage'

const TODOS = ['FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN'] as const
const TI = ['TI_AGENTE', 'TI_ADMIN'] as const

function NaoEncontrado() {
  return (
    <main className="grid min-h-screen place-items-center p-6 text-center">
      <div>
        <h1 className="text-2xl font-bold">Página não encontrada</h1>
        <Link className="mt-4 inline-block text-ocean underline" to="/">
          Voltar ao início
        </Link>
      </div>
    </main>
  )
}

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Entrada />} />
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/meus-chamados"
        element={
          <ExigirPerfil permitido={[...TODOS]}>
            <MeusChamadosPage />
          </ExigirPerfil>
        }
      />
      <Route
        path="/chamados/novo"
        element={
          <ExigirPerfil permitido={[...TODOS]}>
            <NovoChamadoPage />
          </ExigirPerfil>
        }
      />
      <Route
        path="/chamados/:id"
        element={
          <ExigirPerfil permitido={[...TODOS]}>
            <DetalheChamadoPage />
          </ExigirPerfil>
        }
      />
      <Route
        path="/ti/fila"
        element={
          <ExigirPerfil permitido={[...TI]}>
            <FilaTiPage />
          </ExigirPerfil>
        }
      />
      <Route
        path="/ti/dashboard"
        element={
          <ExigirPerfil permitido={[...TI]}>
            <DashboardTiPage />
          </ExigirPerfil>
        }
      />
      <Route
        path="/ti/admin"
        element={
          <ExigirPerfil permitido={['TI_ADMIN']}>
            <AdminLayout />
          </ExigirPerfil>
        }
      >
        <Route path="usuarios" element={<UsuariosAdminPage />} />
        <Route path="categorias" element={<CategoriasAdminPage />} />
        <Route path="slas" element={<SlasAdminPage />} />
        <Route path="calendario" element={<CalendarioAdminPage />} />
      </Route>
      <Route path="*" element={<NaoEncontrado />} />
    </Routes>
  )
}
