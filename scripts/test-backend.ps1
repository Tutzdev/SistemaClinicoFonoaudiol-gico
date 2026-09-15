[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'use-local-env.ps1')
& (Join-Path $PSScriptRoot 'start-local-db.ps1')

# This command is intentionally fixed to the disposable local test database.
# The integration tests independently reject databases not named sinapse_test.
if ($env:TEST_DB_URL -ne 'jdbc:postgresql://127.0.0.1:55432/sinapse_test') {
    throw 'Refusing to run tests outside the isolated local sinapse_test database.'
}
$databaseNames = @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'SPRING_DATASOURCE_URL', 'SPRING_DATASOURCE_USERNAME', 'SPRING_DATASOURCE_PASSWORD')
$previousDatabaseEnvironment = @{}
foreach ($name in $databaseNames) {
    $previousDatabaseEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

if ($env:MAVEN_OPTS -notmatch 'javax\.net\.ssl\.trustStore') {
    $env:MAVEN_OPTS = ('{0} -Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE' -f $env:MAVEN_OPTS).Trim()
}
$mavenCommand = & (Join-Path $PSScriptRoot 'resolve-local-maven.ps1')
$backendRoot = Join-Path $projectRoot 'backend'
Push-Location $backendRoot
try {
    $env:DB_URL = $env:TEST_DB_URL
    $env:DB_USERNAME = $env:TEST_DB_USERNAME
    $env:DB_PASSWORD = $env:TEST_DB_PASSWORD
    $env:SPRING_DATASOURCE_URL = $env:TEST_DB_URL
    $env:SPRING_DATASOURCE_USERNAME = $env:TEST_DB_USERNAME
    $env:SPRING_DATASOURCE_PASSWORD = $env:TEST_DB_PASSWORD
    Write-Host 'Running backend tests against 127.0.0.1:55432/sinapse_test only.'
    & $mavenCommand -B test
    if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed. See backend/target/surefire-reports.' }
}
finally {
    Pop-Location
    foreach ($name in $databaseNames) {
        [Environment]::SetEnvironmentVariable($name, $previousDatabaseEnvironment[$name], 'Process')
    }
}
