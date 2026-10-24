$ErrorActionPreference = "Stop"

docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker no está iniciado. Inicie Docker y vuelva a ejecutar las pruebas."
    Write-Host ""
    Write-Host "========================================"
    Write-Host "❌ Layer 1 integration tests FAILED"
    Write-Host "========================================"
    exit 1
}

Set-Location (Join-Path $PSScriptRoot "..\..")
mvn -pl controlador -am -DskipTests install
$testExit = $LASTEXITCODE
if ($testExit -eq 0) {
    mvn -pl controlador verify -Dgroups=Layer1
    $testExit = $LASTEXITCODE
}

Write-Host ""
Write-Host "========================================"
if ($testExit -eq 0) {
    Write-Host "✅ Layer 1 integration tests PASSED"
} else {
    Write-Host "❌ Layer 1 integration tests FAILED"
}
Write-Host "========================================"
exit $testExit
