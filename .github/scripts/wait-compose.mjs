// Aguarda disponibilidade real da API e do frontend, sem sleep fixo de inicialização.
const limite = Date.now() + 120_000
const urls = ['http://localhost:8080/actuator/health', 'http://localhost:3000/api/v1/auth/config']
for (const url of urls) {
  let pronto = false
  while (Date.now() < limite) {
    try {
      const resposta = await fetch(url, { signal: AbortSignal.timeout(3000) })
      if (resposta.ok) { pronto = true; break }
    } catch { /* Serviço ainda inicializando. */ }
    await new Promise(resolve => setTimeout(resolve, 1000))
  }
  if (!pronto) throw new Error(`Serviço indisponível após 120 segundos: ${url}`)
}
console.log('API real e frontend prontos para E2E')
