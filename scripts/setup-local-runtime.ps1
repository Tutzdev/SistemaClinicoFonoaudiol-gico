[CmdletBinding()]
param(
    [string]$PostgresBin = 'C:\Program Files\PostgreSQL\18\bin'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeRoot = Join-Path $projectRoot '.runtime'
$downloadsRoot = Join-Path $runtimeRoot 'downloads'
$jdkRoot = Join-Path $runtimeRoot 'jdk21'
New-Item -ItemType Directory -Path $runtimeRoot, $downloadsRoot, $jdkRoot -Force | Out-Null

if (-not (Test-Path -LiteralPath (Join-Path $PostgresBin 'initdb.exe'))) {
    throw "PostgreSQL binaries not found at $PostgresBin. Supply -PostgresBin with an existing installation."
}

$installedJdk = Get-ChildItem -LiteralPath $jdkRoot -Directory |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } |
    Select-Object -First 1

if (-not $installedJdk) {
    Write-Host 'Obtaining portable JDK 21 from the official Adoptium release metadata...'
    $asset = @(Invoke-RestMethod -Uri 'https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse')[0]
    $package = $asset.binary.package
    if ($package.link -notlike 'https://github.com/adoptium/temurin21-binaries/releases/download/*' -or $package.checksum -notmatch '^[a-fA-F0-9]{64}$') {
        throw 'Unexpected JDK download source or checksum.'
    }
    $archive = Join-Path $downloadsRoot ([System.IO.Path]::GetFileName($package.name))
    if (-not (Test-Path -LiteralPath $archive)) {
        & curl.exe --fail --location --silent --show-error --retry 3 --output $archive $package.link
        if ($LASTEXITCODE -ne 0) { throw 'JDK download failed.' }
    }
    $actualHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    if ($actualHash -ne $package.checksum) { throw 'JDK SHA-256 verification failed. Archive was not extracted.' }
    Write-Host 'JDK archive SHA-256 verified; extracting locally...'
    & tar.exe -xf $archive -C $jdkRoot
    if ($LASTEXITCODE -ne 0) { throw 'JDK extraction failed.' }
    $asset | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $runtimeRoot 'jdk21-source.json') -Encoding UTF8
}

function New-LocalCredential {
    param([string]$UserName, [string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        $randomBytes = New-Object byte[] 32
        $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try { $random.GetBytes($randomBytes) } finally { $random.Dispose() }
        $securePassword = ConvertTo-SecureString ([Convert]::ToBase64String($randomBytes)) -AsPlainText -Force
        $credential = New-Object System.Management.Automation.PSCredential($UserName, $securePassword)
        $credential | Export-Clixml -LiteralPath $Path
    }
}

New-LocalCredential -UserName 'sinapse_local_admin' -Path (Join-Path $runtimeRoot 'postgres-admin.xml')
New-LocalCredential -UserName 'sinapse' -Path (Join-Path $runtimeRoot 'postgres-app.xml')
New-LocalCredential -UserName 'admin@sinapse.local' -Path (Join-Path $runtimeRoot 'app-admin.xml')
$PostgresBin | Set-Content -LiteralPath (Join-Path $runtimeRoot 'postgres-bin.txt') -Encoding UTF8

Write-Host 'Portable runtime ready. Local credentials are encrypted for the current Windows user in .runtime.'
Write-Host 'Run scripts/start-local-db.ps1 to initialize and start the isolated database.'
