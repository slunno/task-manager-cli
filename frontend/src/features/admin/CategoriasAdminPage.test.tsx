import { cleanup, fireEvent, screen, waitFor } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import { caminho, metodo, montar, resposta, funcionario } from '../../test/app'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

it('admin cria e desativa categoria com CSRF e atualiza a listagem', async () => {
  let categorias = [{ id: 1, nome: 'Rede', ativa: true }]
  const fetch = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const path = caminho(input)
    if (path === '/api/v1/me')
      return resposta({ ...funcionario, perfil: 'TI_ADMIN' })
    if (path === '/api/v1/avisos') return resposta([])
    if (path === '/api/v1/auth/csrf') return resposta({ token: 'csrf-admin' })
    if (
      path === '/api/v1/ti/admin/categorias' &&
      metodo(input, init) === 'POST'
    ) {
      categorias = [...categorias, { id: 2, nome: 'Acessos', ativa: true }]
      return resposta(categorias[1], 201)
    }
    if (
      path === '/api/v1/ti/admin/categorias/2' &&
      metodo(input, init) === 'PATCH'
    ) {
      categorias = categorias.map((c) =>
        c.id === 2 ? { ...c, ativa: false } : c,
      )
      return resposta(categorias[1])
    }
    if (path === '/api/v1/ti/admin/categorias') return resposta(categorias)
    throw new Error(`Rota inesperada: ${path}`)
  })
  vi.stubGlobal('fetch', fetch)
  montar('/ti/admin/categorias')
  fireEvent.change(await screen.findByLabelText('Nova categoria'), {
    target: { value: 'Acessos' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Adicionar' }))
  const texto = await screen.findByText('Acessos', { exact: false })
  fireEvent.click(texto.closest('li')!.querySelector('button')!)
  fireEvent.click(screen.getByLabelText('Ativa para novos chamados'))
  fireEvent.click(screen.getByRole('button', { name: 'Salvar' }))
  await waitFor(() =>
    expect(fetch).toHaveBeenCalledWith(
      '/api/v1/ti/admin/categorias/2',
      expect.objectContaining({
        method: 'PATCH',
        headers: expect.objectContaining({ 'X-CSRF-TOKEN': 'csrf-admin' }),
        body: JSON.stringify({ id: 2, nome: 'Acessos', ativa: false }),
      }),
    ),
  )
  await waitFor(() =>
    expect(
      screen.queryByRole('heading', { name: 'Editar categoria' }),
    ).not.toBeInTheDocument(),
  )
  expect(screen.getByText('(inativa)')).toBeInTheDocument()
})

it('mostra erro de criação sem fechar a administração ou limpar o formulário', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const path = caminho(input)
      if (path === '/api/v1/me')
        return resposta({ ...funcionario, perfil: 'TI_ADMIN' })
      if (path === '/api/v1/auth/csrf') return resposta({ token: 'csrf-admin' })
      if (metodo(input, init) === 'POST')
        return resposta({ detail: 'Categoria já existe' }, 409)
      return resposta([])
    }),
  )
  montar('/ti/admin/categorias')
  fireEvent.change(await screen.findByLabelText('Nova categoria'), {
    target: { value: 'Rede' },
  })
  fireEvent.click(screen.getByRole('button', { name: 'Adicionar' }))
  expect(await screen.findByRole('alert')).toHaveTextContent(
    'Categoria já existe',
  )
  expect(screen.getByLabelText('Nova categoria')).toHaveValue('Rede')
})
