# Script para probar el endpoint /orqcompensacion/v1/transfer
# Ejecuta este script en una terminal PowerShell SEPARADA (mientras el servidor está corriendo)

$headers = @{
    'Content-Type' = 'application/json'
    'nombreOperacion' = 'COBPER'
    'total' = '1'
    'jornada' = '20251222'
    'canal' = '1'
    'modoDeOperacion' = '1'
    'usuario' = 'TESTUSER'
    'perfil' = '1'
    'versionServicio' = '1.0'
    'idTransaccion' = 'test-12345'
}

$body = @'
{
  "NombredelServicio": "TransferenciaService",
  "Data": {
    "codTipoIdentificacion": "CC",
    "valNumeroIdentificacion": "123456789",
    "codTipoProducto": "AHO",
    "valNumeroProducto": "1234567890",
    "codMonedaProducto": "USD",
    "codTipoMovimiento": "DBT",
    "valMonto": 100.00,
    "codMonedaDestino": "USD",
    "codTipoConcepto": "COBPER",
    "valDescripcion": "Prueba transferencia"
  }
}
'@

Write-Host "`n📡 Probando endpoint: POST http://localhost:8081/orqcompensacion/v1/transfer`n" -ForegroundColor Cyan

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8081/orqcompensacion/v1/transfer" -Method POST -Headers $headers -Body $body
    
    Write-Host "✅ Status Code: $($response.StatusCode) $($response.StatusDescription)" -ForegroundColor Green
    Write-Host "`n📥 Response Body:" -ForegroundColor Cyan
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
    
} catch {
    Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "`nStatus Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "`n📥 Error Response Body:" -ForegroundColor Yellow
        Write-Host $responseBody
    }
}

Write-Host "`n" -NoNewline
