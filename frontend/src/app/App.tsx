import { Link, Route, Routes } from 'react-router'
import { AreaPage } from '../features/auth/AreaPage'
import { Entrada, ExigirPerfil } from '../features/auth/Guardas'
import { LoginPage } from '../features/auth/LoginPage'
import { DetalheChamadoPage } from '../features/chamados/DetalheChamadoPage'
import { MeusChamadosPage } from '../features/chamados/MeusChamadosPage'
import { NovoChamadoPage } from '../features/chamados/NovoChamadoPage'

const TODOS = ['FUNCIONARIO', 'TI_AGENTE', 'TI_ADMIN'] as const
const TI = ['TI_AGENTE', 'TI_ADMIN'] as const

function Protegida({
  perfis,
  titulo,
  descricao,
}: {
  perfis: ('FUNCIONARIO' | 'TI_AGENTE' | 'TI_ADMIN')[]
  titulo: string
  descricao: string
}) {
  return (
    <ExigirPerfil permitido={perfis}>
      <AreaPage titulo={titulo} descricao={descricao} />
    </ExigirPerfil>
  )
}

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
          <Protegida
            perfis={[...TI]}
            titulo="Fila da TI"
            descricao="A fila de atendimento será disponibilizada na etapa de operação."
          />
        }
      />
      <Route
        path="/ti/dashboard"
        element={
          <Protegida
            perfis={[...TI]}
            titulo="Dashboard da TI"
            descricao="Os indicadores serão disponibilizados na etapa de SLA e dashboard."
          />
        }
      />
      <Route
        path="/ti/admin/*"
        element={
          <Protegida
            perfis={['TI_ADMIN']}
            titulo="Administração"
            descricao="As configurações serão disponibilizadas na etapa de administração."
          />
        }
      />
      <Route path="*" element={<NaoEncontrado />} />
    </Routes>
  )
}
