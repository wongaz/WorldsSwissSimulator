$ErrorActionPreference = 'Stop'
$envPath = Join-Path $PSScriptRoot '..\.env'
$contents = if (Test-Path -LiteralPath $envPath) {
    [System.IO.File]::ReadAllText($envPath)
} else {
    ''
}
$keys = @('SUPERSET_SECRET_KEY', 'SUPERSET_ADMIN_PASSWORD', 'SUPERSET_READER_PASSWORD')
$added = @()
foreach ($key in $keys) {
    if ($contents -match "(?m)^\s*$key\s*=") {
        continue
    }
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    } finally {
        $rng.Dispose()
    }
    $secret = [System.BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
    if ($contents.Length -gt 0 -and -not $contents.EndsWith("`n")) {
        $contents += "`n"
    }
    $contents += "$key=$secret`n"
    $added += $key
}
if ($added.Count -gt 0) {
    [System.IO.File]::WriteAllText($envPath, $contents, [System.Text.UTF8Encoding]::new($false))
    Write-Output "Added $($added -join ', ') to local .env. Existing values were preserved."
} else {
    Write-Output 'Superset variables already exist in .env; no changes made.'
}
