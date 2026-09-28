import { expect, test } from '@playwright/test'

test('proxy publica health e bloqueia métricas e documentação', async ({
  request,
}) => {
  const health = await request.get('/actuator/health')
  expect(health.status()).toBe(200)
  expect(health.headers()['content-security-policy']).toContain(
    "style-src 'self';",
  )
  expect(health.headers()['content-security-policy']).not.toContain(
    'unsafe-inline',
  )
  expect(await health.json()).toMatchObject({ status: 'UP' })
  for (const path of [
    '/actuator',
    '/actuator/metrics',
    '/actuator/metrics/jvm.memory.used',
    '/v3/api-docs',
    '/v3/api-docs/swagger-config',
    '/swagger-ui/',
    '/swagger-ui.html',
  ]) {
    const response = await request.get(path)
    expect(response.status(), path).toBe(404)
    expect(await response.text(), path).not.toContain('Entrar no Lumeo')
  }
})
