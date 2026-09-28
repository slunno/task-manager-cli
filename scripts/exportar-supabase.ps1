[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$raizProjeto = Split-Path -Parent $PSScriptRoot
Push-Location (Join-Path $raizProjeto 'backend')
try {
    & .\mvnw.cmd -B dependency:build-classpath '-Dmdep.outputFile=target/supabase-classpath.txt'
    if ($LASTEXITCODE -ne 0) { throw 'Nao foi possivel obter o classpath do projeto.' }
    $classpathFlyway = [IO.File]::ReadAllText((Join-Path (Get-Location).Path 'target/supabase-classpath.txt')).Trim()
    & java --class-path $classpathFlyway (Join-Path $PSScriptRoot 'ExportarBootstrapSupabase.java') $raizProjeto
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao exportar o bootstrap Supabase.' }
} finally {
    Pop-Location
}
