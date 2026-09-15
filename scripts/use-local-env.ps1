# Dot-source this script to load project-local settings into this PowerShell only:
# . .\scripts\use-local-env.ps1
$projectRoot = Split-Path -Parent $PSScriptRoot
$runtimeRoot = Join-Path $projectRoot '.runtime'
$jdkRoot = Join-Path $runtimeRoot 'jdk21'

if (-not (Test-Path -LiteralPath $jdkRoot) -or -not (Test-Path -LiteralPath (Join-Path $runtimeRoot 'postgres-app.xml'))) {
    throw 'Local runtime is not initialized. Run scripts/setup-local-runtime.ps1 first.'
}

$jdkDirectory = Get-ChildItem -LiteralPath $jdkRoot -Directory |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\javac.exe') } |
    Select-Object -First 1
if (-not $jdkDirectory) { throw 'Portable JDK 21 was not found.' }

$env:JAVA_HOME = $jdkDirectory.FullName
$jdkBin = Join-Path $env:JAVA_HOME 'bin'
if (($env:Path -split ';') -notcontains $jdkBin) { $env:Path = "$jdkBin;$env:Path" }

$appCredential = Import-Clixml -LiteralPath (Join-Path $runtimeRoot 'postgres-app.xml')
$adminCredential = Import-Clixml -LiteralPath (Join-Path $runtimeRoot 'app-admin.xml')
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://127.0.0.1:55432/sinapse'
$env:SPRING_DATASOURCE_USERNAME = $appCredential.UserName
$env:SPRING_DATASOURCE_PASSWORD = $appCredential.GetNetworkCredential().Password
$env:DB_URL = $env:SPRING_DATASOURCE_URL
$env:DB_USERNAME = $env:SPRING_DATASOURCE_USERNAME
$env:DB_PASSWORD = $env:SPRING_DATASOURCE_PASSWORD
$env:TEST_DB_URL = 'jdbc:postgresql://127.0.0.1:55432/sinapse_test'
$env:TEST_DB_USERNAME = $appCredential.UserName
$env:TEST_DB_PASSWORD = $appCredential.GetNetworkCredential().Password
$env:BOOTSTRAP_ADMIN_EMAIL = $adminCredential.UserName
$env:BOOTSTRAP_ADMIN_PASSWORD = $adminCredential.GetNetworkCredential().Password
$env:BOOTSTRAP_ADMIN_NAME = 'Administracao local'
$env:SERVER_ADDRESS = '127.0.0.1'
$env:SERVER_PORT = '8081'
$env:API_BASE_URL = 'http://127.0.0.1:8081'
$env:API_PROXY_TARGET = $env:API_BASE_URL
