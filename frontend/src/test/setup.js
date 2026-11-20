// Registers the jest-dom matchers (toBeInTheDocument, toHaveTextContent, ...)
// with Vitest's expect, so component assertions read the same way they would
// under Jest.
import '@testing-library/jest-dom/vitest'

// Testing Library only cleans up automatically when the test framework exposes
// `afterEach` as a global. Vitest runs with globals disabled here, so the
// teardown is registered once in this setup file instead of in every test.
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

afterEach(() => {
  cleanup()
})
