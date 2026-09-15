[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeRoot = Join-Path $projectRoot '.runtime'
$dataRoot = Join-Path $runtimeRoot 'postgres-data'
$logPath = Join-Path $runtimeRoot 'postgres.log'
$binaryPathFile = Join-Path $runtimeRoot 'postgres-bin.txt'
if (-not (Test-Path -LiteralPath $binaryPathFile)) {
    throw 'Run scripts/setup-local-runtime.ps1 first.'
}
$postgresBin = (Get-Content -LiteralPath $binaryPathFile -Raw).Trim()
$pgCtl = Join-Path $postgresBin 'pg_ctl.exe'
$psql = Join-Path $postgresBin 'psql.exe'
$adminCredential = Import-Clixml -LiteralPath (Join-Path $runtimeRoot 'postgres-admin.xml')
$appCredential = Import-Clixml -LiteralPath (Join-Path $runtimeRoot 'postgres-app.xml')

if (-not (Test-Path -LiteralPath (Join-Path $dataRoot 'PG_VERSION'))) {
    $passwordFile = Join-Path $runtimeRoot 'initdb-password.tmp'
    [System.IO.File]::WriteAllText($passwordFile, $adminCredential.GetNetworkCredential().Password)
    try {
        & (Join-Path $postgresBin 'initdb.exe') --pgdata=$dataRoot --username=$($adminCredential.UserName) --pwfile=$passwordFile --auth-host=scram-sha-256 --auth-local=scram-sha-256 --encoding=UTF8 --locale=C
        if ($LASTEXITCODE -ne 0) { throw 'Initializing local PostgreSQL failed.' }
    }
    finally {
        $resolvedPasswordFile = [System.IO.Path]::GetFullPath($passwordFile)
        if ($resolvedPasswordFile.StartsWith([System.IO.Path]::GetFullPath($runtimeRoot) + [System.IO.Path]::DirectorySeparatorChar)) {
            Remove-Item -LiteralPath $resolvedPasswordFile -Force -ErrorAction SilentlyContinue
        }
    }
}

& $pgCtl status -D $dataRoot *> $null
$clusterRunning = $LASTEXITCODE -eq 0
if (-not $clusterRunning) {
    $portInUse = Get-NetTCPConnection -State Listen -LocalPort 55432 -ErrorAction SilentlyContinue
    if ($portInUse) { throw 'Port 55432 is already in use; the existing listener was not modified.' }
    $arguments = '-D "{0}" -l "{1}" -o "-h 127.0.0.1 -p 55432 -c log_statement=none -c log_min_error_statement=panic" -w -t 30 start' -f $dataRoot, $logPath
    $startup = Start-Process -FilePath $pgCtl -ArgumentList $arguments -WindowStyle Hidden -PassThru
    if (-not $startup.WaitForExit(35000)) { throw 'Timed out waiting for local PostgreSQL startup.' }
    $startup.Refresh()
    if ($startup.ExitCode -ne 0) { throw 'Local PostgreSQL failed to start. Inspect .runtime/postgres.log.' }
}

$previousPgPassword = $env:PGPASSWORD
$previousAppPassword = $env:SINAPSE_LOCAL_DB_PASSWORD
try {
    $env:PGPASSWORD = $adminCredential.GetNetworkCredential().Password
    $env:SINAPSE_LOCAL_DB_PASSWORD = $appCredential.GetNetworkCredential().Password
    $connection = @('-h', '127.0.0.1', '-p', '55432', '-U', $adminCredential.UserName, '-d', 'postgres', '-X', '-v', 'ON_ERROR_STOP=1')
    $roleExists = & $psql @connection -tAc "SELECT 1 FROM pg_roles WHERE rolname = 'sinapse'"
    if ($LASTEXITCODE -ne 0) { throw 'Database administrator authentication failed.' }
    if (($roleExists | Out-String).Trim() -ne '1') {
        @'
\getenv app_password SINAPSE_LOCAL_DB_PASSWORD
CREATE ROLE sinapse LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'app_password';
'@ | & $psql @connection -f -
        if ($LASTEXITCODE -ne 0) { throw 'Creating local application role failed.' }
    }
    $databaseExists = & $psql @connection -tAc "SELECT 1 FROM pg_database WHERE datname = 'sinapse'"
    if ($LASTEXITCODE -ne 0) { throw 'Database existence check failed.' }
    if (($databaseExists | Out-String).Trim() -ne '1') {
        & $psql @connection -c 'CREATE DATABASE sinapse OWNER sinapse'
        if ($LASTEXITCODE -ne 0) { throw 'Creating local application database failed.' }
    }
    $testDatabaseExists = & $psql @connection -tAc "SELECT 1 FROM pg_database WHERE datname = 'sinapse_test'"
    if ($LASTEXITCODE -ne 0) { throw 'Test database existence check failed.' }
    if (($testDatabaseExists | Out-String).Trim() -ne '1') {
        & $psql @connection -c 'CREATE DATABASE sinapse_test OWNER sinapse'
        if ($LASTEXITCODE -ne 0) { throw 'Creating local test database failed.' }
    }
    $env:PGPASSWORD = $appCredential.GetNetworkCredential().Password
    & $psql -h 127.0.0.1 -p 55432 -U sinapse -d sinapse -X -v ON_ERROR_STOP=1 -tAc 'SELECT 1' | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Application database readiness check failed.' }
}
finally {
    $env:PGPASSWORD = $previousPgPassword
    $env:SINAPSE_LOCAL_DB_PASSWORD = $previousAppPassword
}
Write-Host 'PostgreSQL is ready at 127.0.0.1:55432; database/user: sinapse. No global service was installed.'
