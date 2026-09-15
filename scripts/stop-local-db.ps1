[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeRoot = Join-Path $projectRoot '.runtime'
$dataRoot = Join-Path $runtimeRoot 'postgres-data'
$binaryPathFile = Join-Path $runtimeRoot 'postgres-bin.txt'
if (-not (Test-Path -LiteralPath $binaryPathFile) -or -not (Test-Path -LiteralPath (Join-Path $dataRoot 'PG_VERSION'))) {
    Write-Host 'There is no initialized project-local PostgreSQL cluster.'
    return
}
$postgresBin = (Get-Content -LiteralPath $binaryPathFile -Raw).Trim()
$pgCtl = Join-Path $postgresBin 'pg_ctl.exe'
& $pgCtl status -D $dataRoot *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host 'The project-local PostgreSQL cluster is already stopped.'
    return
}
$arguments = '-D "{0}" -m fast -w -t 30 stop' -f $dataRoot
$shutdown = Start-Process -FilePath $pgCtl -ArgumentList $arguments -WindowStyle Hidden -PassThru
if (-not $shutdown.WaitForExit(35000)) { throw 'Timed out waiting for local PostgreSQL shutdown.' }
$shutdown.Refresh()
if ($shutdown.ExitCode -ne 0) { throw 'The project-local PostgreSQL cluster did not stop successfully.' }
Write-Host 'Project-local PostgreSQL stopped. Its data remains in .runtime/postgres-data.'
