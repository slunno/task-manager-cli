import { Route, Routes } from 'react-router'

function Inicio() {
  return (
    <main className="min-h-screen bg-[#f7f9f8] text-ink">
      <div className="mx-auto flex min-h-screen max-w-5xl flex-col px-5 py-6 sm:px-8">
        <header className="flex items-center justify-between border-b border-slate-200 pb-5">
          <div className="flex items-center gap-3">
            <span
              aria-hidden="true"
              className="grid h-10 w-10 place-items-center rounded-xl bg-ocean text-lg font-bold text-white"
            >
              TI
            </span>
            <span className="font-semibold tracking-tight">
              Portal de chamados
            </span>
          </div>
          <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-600">
            Fundação
          </span>
        </header>
        <section className="grid flex-1 items-center gap-10 py-16 md:grid-cols-[1.2fr_0.8fr]">
          <div>
            <p className="mb-4 text-sm font-semibold uppercase tracking-[0.2em] text-ocean">
              TI interna
            </p>
            <h1 className="max-w-2xl text-4xl font-bold leading-tight tracking-tight sm:text-5xl">
              Um lugar simples para pedir ajuda e acompanhar cada chamado.
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-8 text-slate-600">
              A estrutura do portal está pronta. O acesso corporativo e os
              fluxos de atendimento serão liberados nas próximas etapas.
            </p>
          </div>
          <div className="rounded-3xl border border-slate-200 bg-white p-7 shadow-lg">
            <h2 className="mb-6 text-lg font-semibold">Etapas da entrega</h2>
            <ol className="space-y-5">
              <li className="flex gap-4">
                <span className="step current">1</span>
                <span>
                  <strong className="block">Fundação</strong>
                  <span className="text-sm text-slate-500">
                    Arquitetura e ambiente
                  </span>
                </span>
              </li>
              <li className="flex gap-4">
                <span className="step">2</span>
                <span>
                  <strong className="block">Identidade</strong>
                  <span className="text-sm text-slate-500">
                    Acesso corporativo e perfis
                  </span>
                </span>
              </li>
              <li className="flex gap-4">
                <span className="step">3</span>
                <span>
                  <strong className="block">Chamados</strong>
                  <span className="text-sm text-slate-500">
                    Abertura e acompanhamento
                  </span>
                </span>
              </li>
            </ol>
          </div>
        </section>
        <footer className="border-t border-slate-200 pt-5 text-sm text-slate-500">
          Portal interno de TI · Horários exibidos em America/Sao_Paulo
        </footer>
      </div>
    </main>
  )
}

function NaoEncontrado() {
  return (
    <main className="grid min-h-screen place-items-center p-6 text-center">
      <div>
        <h1 className="text-2xl font-bold">Página não encontrada</h1>
        <a className="mt-4 inline-block text-ocean underline" href="/">
          Voltar ao início
        </a>
      </div>
    </main>
  )
}

export function App() {
  return (
    <Routes>
      <Route path="/" element={<Inicio />} />
      <Route path="*" element={<NaoEncontrado />} />
    </Routes>
  )
}
