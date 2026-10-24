$ports = 1883, 9001, 8080, 8081, 8082, 8083, 8090, 8091

foreach ($port in $ports) {
    $processIds = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique

    if (-not $processIds) {
        Write-Host "Puerto $port libre"
        continue
    }

    foreach ($processId in $processIds) {
        try {
            Stop-Process -Id $processId -Force -ErrorAction Stop
            Write-Host "Proceso $processId terminado en el puerto $port"
        }
        catch {
            Write-Warning "No se pudo terminar el proceso $processId del puerto ${port}: $($_.Exception.Message)"
        }
    }
}
