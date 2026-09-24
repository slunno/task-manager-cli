import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

const gerado = JSON.parse(readFileSync('backend/target/openapi.json', 'utf8'))
const versionado = JSON.parse(readFileSync('frontend/openapi.json', 'utf8'))
assert.deepStrictEqual(gerado, versionado, 'OpenAPI versionado está desatualizado')
