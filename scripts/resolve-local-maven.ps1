# Returns a Maven executable without changing or installing global tools.
$projectRoot = Split-Path -Parent $PSScriptRoot
$wrapperPath = Join-Path $projectRoot 'backend\mvnw.cmd'
if (Test-Path -LiteralPath $wrapperPath) {
    return $wrapperPath
}
$installedMaven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($installedMaven) {
    return $installedMaven.Source
}
$mavenCache = Join-Path ([System.Environment]::GetFolderPath('UserProfile')) '.m2\wrapper\dists\apache-maven-3.9.16'
$cachedMaven = Get-ChildItem -LiteralPath $mavenCache -Directory -ErrorAction SilentlyContinue |
    Where-Object { Test-Path -LiteralPath (Join-Path $_.FullName 'bin\mvn.cmd') } |
    Select-Object -First 1
if (-not $cachedMaven) {
    throw 'Maven was not found. Install Maven 3.9.x or restore the official backend/mvnw.cmd wrapper.'
}
return (Join-Path $cachedMaven.FullName 'bin\mvn.cmd')
