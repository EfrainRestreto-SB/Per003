@echo off
echo.
echo Probando endpoint: POST http://localhost:8081/orqcompensacion/v1/transfer
echo.

powershell -Command "$headers = @{'Content-Type'='application/json';'nombreOperacion'='COBPER';'total'='1';'jornada'='20251222';'canal'='1';'modoDeOperacion'='1';'usuario'='TESTUSER';'perfil'='1';'versionServicio'='1.0';'idTransaccion'='test-12345'}; $body = '{\"NombredelServicio\":\"TransferenciaService\",\"Data\":{\"codTipoIdentificacion\":\"CC\",\"valNumeroIdentificacion\":\"123456789\",\"codTipoProducto\":\"AHO\",\"valNumeroProducto\":\"1234567890\",\"codMonedaProducto\":\"USD\",\"codTipoMovimiento\":\"DBT\",\"valMonto\":100.00,\"codMonedaDestino\":\"USD\",\"codTipoConcepto\":\"COBPER\",\"valDescripcion\":\"Prueba transferencia\"}}'; try { $response = Invoke-WebRequest -Uri 'http://localhost:8081/orqcompensacion/v1/transfer' -Method POST -Headers $headers -Body $body; Write-Host 'Status:' $response.StatusCode -ForegroundColor Green; Write-Host ''; Write-Host 'Response:'; $response.Content } catch { Write-Host 'Error:' $_.Exception.Message -ForegroundColor Red; Write-Host 'Status Code:' $_.Exception.Response.StatusCode.value__; if ($_.Exception.Response) { $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream()); Write-Host ''; Write-Host 'Response Body:'; $reader.ReadToEnd() } }"

echo.
pause
