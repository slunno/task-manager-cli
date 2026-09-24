import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { App } from './App'

describe('estrutura inicial', () => {
  it('apresenta o estado da fundação', () => {
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    )
    expect(
      screen.getByRole('heading', { name: /um lugar simples/i }),
    ).toBeInTheDocument()
    expect(screen.getByText('Arquitetura e ambiente')).toBeInTheDocument()
  })
})
