param(
    [string]$EnvironmentFile = (Join-Path $PSScriptRoot '..\.env'),
    [string]$CredentialsFile = (Join-Path $PSScriptRoot '..\.env.lumeo-validacao')
)

$ErrorActionPreference = 'Stop'
$config = @{}
Get-Content -LiteralPath $EnvironmentFile | ForEach-Object {
    if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') {
        $config[$matches[1]] = $matches[2].Trim().Trim('"').Trim("'")
    }
}
if (-not $config['SUPABASE_SECRET_KEY']) { throw 'Configure SUPABASE_SECRET_KEY no arquivo de ambiente local.' }
$baseUrl = $config['SUPABASE_URL'].TrimEnd('/')
if ($baseUrl -ne 'https://evsmgbziifqbyahvcwsc.supabase.co') {
    throw 'O arquivo de ambiente deve apontar para o Projeto de chamados.'
}
$headers = @{ apikey = $config['SUPABASE_SECRET_KEY']; Authorization = ('Bearer ' + $config['SUPABASE_SECRET_KEY']) }
$previous = @{}
if (Test-Path -LiteralPath $CredentialsFile) {
    Get-Content -LiteralPath $CredentialsFile | ForEach-Object {
        if ($_ -match '^([A-Z][A-Z0-9_]*)=(.*)$') { $previous[$matches[1]] = $matches[2].Trim() }
    }
}
$accounts = @(
    @{ Prefix = 'TI'; Email = 'ti.validacao@lumeo.invalid'; Name = 'TI — Validação Lumeo' },
    @{ Prefix = 'USUARIO'; Email = 'usuario.validacao@lumeo.invalid'; Name = 'Pessoa — Validação Lumeo' }
)
$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add('# Credenciais locais de validação. Não compartilhar nem versionar.')
foreach ($account in $accounts) {
    $passwordKey = $account.Prefix + '_PASSWORD'
    $password = $previous[$passwordKey]
    $knownId = $previous[$account.Prefix + '_AUTH_ID']
    if (-not $knownId) {
        $bytes = New-Object byte[] 24
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
        $password = 'Lm9!' + [Convert]::ToBase64String($bytes)
        # Salvar antes da requisição para permitir recuperar a senha se houver uma interrupção.
        $lines.Add($account.Prefix + '_EMAIL=' + $account.Email)
        $lines.Add($passwordKey + '=' + $password)
        [System.IO.File]::WriteAllLines([System.IO.Path]::GetFullPath($CredentialsFile), $lines)
        $body = @{ email = $account.Email; password = $password; email_confirm = $true; user_metadata = @{ name = $account.Name } } | ConvertTo-Json -Depth 4
        try {
            $user = Invoke-RestMethod -Method Post -Uri ($baseUrl + '/auth/v1/admin/users') -Headers $headers -ContentType 'application/json' -Body $body
        } catch {
            throw ('Não foi possível criar a conta ' + $account.Email + '. Consulte o Supabase Auth antes de tentar novamente; nenhuma resposta sensível foi exibida.')
        }
        $knownId = $user.id
        if (-not $knownId) { throw 'O Supabase não confirmou a identidade criada.' }
    } else {
        $lines.Add($account.Prefix + '_EMAIL=' + $account.Email)
        $lines.Add($passwordKey + '=' + $password)
    }
    $lines.Add($account.Prefix + '_AUTH_ID=' + $knownId)
    [System.IO.File]::WriteAllLines([System.IO.Path]::GetFullPath($CredentialsFile), $lines)
    try {
        $session = Invoke-RestMethod -Method Post -Uri ($baseUrl + '/auth/v1/token?grant_type=password') -Headers @{ apikey = $config['SUPABASE_PUBLISHABLE_KEY'] } -ContentType 'application/json' -Body (@{email=$account.Email;password=$password} | ConvertTo-Json)
    } catch { throw ('A conta ' + $account.Email + ' foi criada, mas o login não pôde ser verificado.') }
    if ($session.user.id -ne $knownId) { throw 'A identidade retornada no login não corresponde à conta criada.' }
    Write-Output ($account.Email + ': conta Auth criada e login com senha verificado.')
    $session = $null
}
Write-Output ('Senhas salvas somente em: ' + [System.IO.Path]::GetFullPath($CredentialsFile))
