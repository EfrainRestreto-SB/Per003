# Script para validar la auditoría del microservicio PER003
# Ejecutar en una terminal SEPARADA mientras el servidor está corriendo

Write-Host "`n=== PRUEBA DE VALIDACIÓN DE AUDITORÍA PER003 ===" -ForegroundColor Cyan
Write-Host "Este script hará una petición al endpoint y luego mostrará los logs de auditoría`n" -ForegroundColor Yellow

$headers = @{
    'Content-Type' = 'application/json'
    'nombreOperacion' = 'COBPER'
    'total' = '1'
    'jornada' = '20260106'
    'canal' = '1'
    'modoDeOperacion' = '1'
    'usuario' = 'TESTUSER'
    'perfil' = '1'
    'versionServicio' = '1.0'
    'idTransaccion' = 'test-audit-' + (Get-Date -Format 'yyyyMMddHHmmss')
}

$body = @'
{
  "NombredelServicio": "TransferenciaService",
  "Data": {
    "codTipoIdentificacion": "CC",
    "valNumeroIdentificacion": "987654321",
    "codTipoProducto": "AHO",
    "valNumeroProducto": "1234567890",
    "codMonedaProducto": "USD",
    "codTipoMovimiento": "DBT",
    "valMonto": 100.00,
    "codMonedaDestino": "USD",
    "codTipoConcepto": "COBPER",
    "valDescripcion": "Prueba de validacion de auditoria"
  }
}
'@

Write-Host "📡 Enviando petición POST a http://localhost:8080/orqcompensacion/v1/transfer" -ForegroundColor Cyan
Write-Host "ID Transacción: $($headers.idTransaccion)`n" -ForegroundColor Green

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/orqcompensacion/v1/transfer" `
                                   -Method POST `
                                   -Headers $headers `
                                   -Body $body `
                                   -ContentType 'application/json'
    
    Write-Host "✅ Respuesta exitosa - Status Code: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "`n📥 Response Body:" -ForegroundColor Cyan
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
    
    Write-Host "`n`n✅ VALIDACIÓN EXITOSA" -ForegroundColor Green
    Write-Host "El sistema de auditoría debería haber registrado:" -ForegroundColor Yellow
    Write-Host "  1. LOG de ENTRADA con el TransferCommand serializado" -ForegroundColor White
    Write-Host "  2. LOG de SALIDA con el TransferResult serializado" -ForegroundColor White
    Write-Host "`nRevisa la consola del servidor Quarkus para ver los logs de auditoría." -ForegroundColor Yellow
    Write-Host "O consulta la tabla PERUSRLIB.AUDIT_LOGS en DB2 para ver los registros." -ForegroundColor Yellow
    
} catch {
    Write-Host "❌ Error al hacer la petición: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "`nStatus Code: $statusCode" -ForegroundColor Yellow
        
        try {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "`n📥 Error Response Body:" -ForegroundColor Yellow
            Write-Host $responseBody
            
            Write-Host "`n`n⚠️ VALIDACIÓN CON ERROR" -ForegroundColor Yellow
            Write-Host "El sistema de auditoría debería haber registrado:" -ForegroundColor Yellow
            Write-Host "  1. LOG de ENTRADA con el TransferCommand serializado" -ForegroundColor White
            Write-Host "  2. LOG de ERROR con la excepción serializada" -ForegroundColor White
            
        } catch {
            Write-Host "No se pudo leer el cuerpo de la respuesta de error" -ForegroundColor Red
        }
    }
}

Write-Host "`n"
