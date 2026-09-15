[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$credentialPath = Join-Path $projectRoot '.runtime\app-admin.xml'
if (-not (Test-Path -LiteralPath $credentialPath)) {
    throw 'Execute scripts/setup-local-runtime.ps1 antes de consultar o acesso local.'
}

# Run intentionally in your own terminal. This script displays a bootstrap secret;
# it must not be used in CI output, shared logs, screenshots or recorded demos.
$credential = Import-Clixml -LiteralPath $credentialPath
Write-Host 'Acesso administrativo local - credenciais do primeiro inicio'
Write-Host ('E-mail: {0}' -f $credential.UserName)
Write-Host ('Senha: {0}' -f $credential.GetNetworkCredential().Password)
Write-Host 'Se a senha ja foi alterada no sistema, utilize a senha atual.'
