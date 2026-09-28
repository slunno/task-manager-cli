[CmdletBinding()]
param(
    [ValidateSet('dev', 'prod')]
    [string]$Perfil = 'dev',
    [string]$ArquivoAmbiente,
    [switch]$ValidarConfiguracao
)

$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
if (-not $ArquivoAmbiente) {
    $ArquivoAmbiente = Join-Path $raizProjeto '.env.supabase'
}
if (-not (Test-Path -LiteralPath $ArquivoAmbiente -PathType Leaf)) {
    throw 'Crie .env.supabase na raiz usando .env.supabase.example e preencha SUPABASE_DB_PASSWORD.'
}

$variaveisAnteriores = @{}
function Definir-VariavelLocal([string]$Nome, [string]$Valor) {
    if (-not $variaveisAnteriores.ContainsKey($Nome)) {
        $variaveisAnteriores[$Nome] = [Environment]::GetEnvironmentVariable($Nome, 'Process')
    }
    [Environment]::SetEnvironmentVariable($Nome, $Valor, 'Process')
}

try {
    $numeroLinha = 0
    foreach ($linhaAmbiente in [IO.File]::ReadAllLines((Resolve-Path -LiteralPath $ArquivoAmbiente).Path)) {
        $numeroLinha++
        if ($linhaAmbiente -match '^\s*(#|$)') { continue }
        if ($linhaAmbiente -notmatch '^\s*([A-Z][A-Z0-9_]*)=(.*)$') {
            throw "Formato invalido no arquivo de ambiente, linha $numeroLinha. Use NOME=valor."
        }
        $nomeVariavel = $Matches[1]
        $valorVariavel = $Matches[2].Trim()
        if ($nomeVariavel -notmatch '^(SUPABASE_DB_[A-Z_]+|SMTP_[A-Z_]+|HELPDESK_[A-Z_]+|OIDC_[A-Z_]+|PORT)$') {
            throw "Variavel nao permitida no arquivo de ambiente, linha $numeroLinha."
        }
        if ($valorVariavel.Length -ge 2) {
            $primeiroCaractere = $valorVariavel.Substring(0, 1)
            $ultimoCaractere = $valorVariavel.Substring($valorVariavel.Length - 1, 1)
            if (($primeiroCaractere -eq '"' -or $primeiroCaractere -eq "'") -and $primeiroCaractere -eq $ultimoCaractere) {
                $valorVariavel = $valorVariavel.Substring(1, $valorVariavel.Length - 2)
            }
        }
        Definir-VariavelLocal $nomeVariavel $valorVariavel
    }
    if ([string]::IsNullOrWhiteSpace($env:SUPABASE_DB_PASSWORD) -or $env:SUPABASE_DB_PASSWORD -eq '[YOUR-PASSWORD]') {
        throw 'Preencha SUPABASE_DB_PASSWORD em .env.supabase antes de iniciar. A senha nao sera exibida.'
    }
    if ($env:SUPABASE_DB_JDBC_URL) {
        if ($env:SUPABASE_DB_JDBC_URL -notmatch '^jdbc:postgresql://' -or $env:SUPABASE_DB_JDBC_URL -notmatch '[?&]sslmode=(require|verify-ca|verify-full)(&|$)') {
            throw 'A URL deve ser JDBC PostgreSQL com sslmode=require, verify-ca ou verify-full.'
        }
        if ($env:SUPABASE_DB_JDBC_URL -match ':6543/') {
            throw 'Use conexao direta ou Session pooler na porta 5432 para Hibernate/Flyway.'
        }
        if ($env:SUPABASE_DB_JDBC_URL -match '[?&](password|user)=') {
            throw 'Defina usuario e senha nas variaveis separadas, sem credenciais na URL.'
        }
    }
    Definir-VariavelLocal 'SPRING_PROFILES_ACTIVE' "$Perfil,supabase"
    if ($Perfil -eq 'dev') { Definir-VariavelLocal 'SERVER_ADDRESS' '127.0.0.1' }
    if ($ValidarConfiguracao) {
        Write-Output "Arquivo de ambiente validado; perfis $Perfil,supabase. Nenhuma conexao foi aberta."
        return
    }
    Push-Location (Join-Path $raizProjeto 'backend')
    try {
        & .\mvnw.cmd spring-boot:run
        if ($LASTEXITCODE -ne 0) { throw 'O backend encerrou com erro; confira o diagnostico acima.' }
    } finally {
        Pop-Location
    }
} finally {
    foreach ($nomeAnterior in $variaveisAnteriores.Keys) {
        if ($null -eq $variaveisAnteriores[$nomeAnterior]) {
            Remove-Item -LiteralPath "Env:$nomeAnterior" -ErrorAction SilentlyContinue
        } else {
            [Environment]::SetEnvironmentVariable($nomeAnterior, $variaveisAnteriores[$nomeAnterior], 'Process')
        }
    }
}
