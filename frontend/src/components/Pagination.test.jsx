import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import Pagination from './Pagination.jsx'

describe('Pagination', () => {
  it('renders a singular summary and no buttons for a single page', () => {
    render(<Pagination page={0} totalPages={1} totalElements={1} onPage={vi.fn()} />)

    expect(screen.getByText('1 opportunity')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('renders a plural summary and no buttons for a single page', () => {
    render(<Pagination page={0} totalPages={1} totalElements={3} onPage={vi.fn()} />)

    expect(screen.getByText('3 opportunities')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })

  it('renders nothing when there is no page count and no total', () => {
    const { container } = render(<Pagination page={0} totalPages={0} totalElements={null} onPage={vi.fn()} />)

    expect(container).toBeEmptyDOMElement()
  })

  it('renders the page indicator and the total for multiple pages', () => {
    render(<Pagination page={0} totalPages={3} totalElements={42} onPage={vi.fn()} />)

    expect(screen.getByText('1 / 3 · 42 total')).toBeInTheDocument()
  })

  it('disables Prev on the first page', () => {
    render(<Pagination page={0} totalPages={3} totalElements={42} onPage={vi.fn()} />)

    expect(screen.getByRole('button', { name: 'Prev' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Next' })).toBeEnabled()
  })

  it('disables Next on the last page', () => {
    render(<Pagination page={2} totalPages={3} totalElements={42} onPage={vi.fn()} />)

    expect(screen.getByRole('button', { name: 'Next' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Prev' })).toBeEnabled()
  })

  it('asks for the previous page when Prev is clicked', async () => {
    const user = userEvent.setup()
    const onPage = vi.fn()
    render(<Pagination page={1} totalPages={3} totalElements={42} onPage={onPage} />)

    await user.click(screen.getByRole('button', { name: 'Prev' }))

    expect(onPage).toHaveBeenCalledTimes(1)
    expect(onPage).toHaveBeenCalledWith(0)
  })

  it('asks for the next page when Next is clicked', async () => {
    const user = userEvent.setup()
    const onPage = vi.fn()
    render(<Pagination page={1} totalPages={3} totalElements={42} onPage={onPage} />)

    await user.click(screen.getByRole('button', { name: 'Next' }))

    expect(onPage).toHaveBeenCalledTimes(1)
    expect(onPage).toHaveBeenCalledWith(2)
  })
})
