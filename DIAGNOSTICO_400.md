# 🔧 Diagnóstico Error 400 Bad Request

## ✅ Confirmación: El Endpoint Funciona

He verificado que el endpoint `/orqcompensacion/v1/transfer` **SÍ está disponible** en `http://localhost:8080` y responde correctamente cuando se envía la petición completa.

---

## ❌ Causas del Error 400

El error 400 Bad Request ocurre cuando **falta algún header HTTP requerido** o tiene un **valor inválido**.

### Headers Obligatorios (Todos deben estar presentes)

| Header | Tipo | Ejemplo | Validación |
|--------|------|---------|------------|
| `Content-Type` | String | `application/json` | Debe ser exactamente `application/json` |
| `nombreOperacion` | String | `COBPER` | No puede estar vacío |
| `total` | Integer | `1` | Debe ser un número entero |
| `jornada` | Short | `20260106` | Debe ser un número (formato YYYYMMDD) |
| `canal` | Short | `1` | Debe ser un número entero |
| `modoDeOperacion` | Short | `1` | Debe ser un número entero |
| `usuario` | String | `TESTUSER` | No puede estar vacío |
| `perfil` | Short | `1` | Debe ser un número entero |
| `versionServicio` | String | `1.0` | No puede estar vacío |
| `idTransaccion` | String | `test-001` | No puede estar vacío |

---

## 🔍 Checklist de Diagnóstico en Postman

### 1. Verificar la URL
- [ ] **URL completa**: `http://localhost:8080/orqcompensacion/v1/transfer`
- [ ] **Método**: `POST` (NO GET, NO PUT)
- [ ] **Sin espacios extras en la URL**

### 2. Verificar los Headers

En Postman, pestaña **Headers**:

1. **Cuenta los headers**: Debes tener **EXACTAMENTE 10 headers**
   - Content-Type
   - nombreOperacion
   - total
   - jornada
   - canal
   - modoDeOperacion
   - usuario
   - perfil
   - versionServicio
   - idTransaccion

2. **Verifica que todos estén marcados (checkbox activado)**
   - A veces Postman desactiva headers automáticamente
   - Revisa la columna de checkboxes a la izquierda de cada header

3. **Verifica la capitalización exacta de los nombres**
   - `nombreOperacion` (NO `nombreoperacion`, NO `NombreOperacion`)
   - `modoDeOperacion` (NO `mododeoperacion`)
   - `versionServicio` (NO `versionservicio`)
   - `idTransaccion` (NO `idtransaccion`)

4. **Verifica que los valores numéricos NO tengan comillas en Postman**
   - Correcto: `1` (sin comillas)
   - Incorrecto: `"1"` (con comillas) - aunque en teoría debería funcionar

### 3. Verificar el Body

En Postman, pestaña **Body**:

- [ ] Seleccionado: `raw`
- [ ] Dropdown a la derecha: `JSON`
- [ ] El JSON debe tener la estructura exacta:

```json
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
    "valDescripcion": "Prueba de auditoria desde Postman"
  }
}
```

---

## 🧪 Prueba Rápida con PowerShell

Para confirmar que el servidor funciona, ejecuta este comando en PowerShell:

```powershell
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
    'idTransaccion' = 'test-powershell-001'
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
    "valDescripcion": "Test desde PowerShell"
  }
}
'@

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/orqcompensacion/v1/transfer" `
        -Method POST `
        -Headers $headers `
        -Body $body

    Write-Host "✅ ÉXITO - Status 200" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 5
} catch {
    Write-Host "❌ ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Yellow
}
```

**Si este script funciona pero Postman no**, entonces el problema está específicamente en la configuración de Postman.

---

## 📸 Comparación: Postman vs Esperado

### Lo que Postman debe mostrar ANTES de enviar:

**Headers Tab:**
```
KEY                  VALUE
Content-Type         application/json     ☑
nombreOperacion      COBPER              ☑
total                1                   ☑
jornada              20260106            ☑
canal                1                   ☑
modoDeOperacion      1                   ☑
usuario              TESTUSER            ☑
perfil               1                   ☑
versionServicio      1.0                 ☑
idTransaccion        test-postman-001    ☑
```

(Nota: El ☑ indica que el checkbox debe estar marcado)

---

## 🔍 Errores Comunes en Postman

### Error #1: Headers desactivados
**Síntoma**: Los headers aparecen pero con checkbox desmarcado  
**Solución**: Activar todos los checkboxes

### Error #2: Capitalización incorrecta
**Síntoma**: `nombreoperacion` en lugar de `nombreOperacion`  
**Solución**: Copiar exactamente los nombres de la tabla anterior

### Error #3: Headers en el Body
**Síntoma**: Intentar enviar los headers dentro del JSON  
**Solución**: Los headers van en la pestaña "Headers", NO en el Body

### Error #4: Content-Type en Body
**Síntoma**: El dropdown de Body no está en "JSON"  
**Solución**: Seleccionar "raw" y luego "JSON" en el dropdown

### Error #5: Puerto incorrecto
**Síntoma**: Usar `http://localhost:8081` en lugar de `http://localhost:8080`  
**Solución**: Verificar que el puerto es **8080**

---

## 📋 Pasos para Resolver

1. **Cierra y vuelve a abrir la petición en Postman**
   - A veces Postman tiene problemas de caché

2. **Crea una nueva petición desde cero**
   - Click en "New" → "HTTP Request"
   - Configura todo paso a paso

3. **Importa la colección JSON**
   - Usa el JSON de colección en [POSTMAN_COLLECTION.md](POSTMAN_COLLECTION.md)
   - Click en "Import" → Pegar el JSON

4. **Verifica en la consola de Postman**
   - Abre "View" → "Show Postman Console"
   - Haz la petición
   - Revisa qué headers y body se están enviando realmente

5. **Compara con curl**
   - Si el script PowerShell funciona pero Postman no
   - Exporta la petición de Postman como cURL
   - Compara con el comando que funciona

---

## 🆘 Si Nada Funciona

1. **Exporta la petición de Postman**
   - Click en "Code" en la esquina derecha
   - Selecciona "cURL"
   - Copia y pega el comando aquí

2. **Toma una captura de pantalla**
   - De la pestaña Headers completa
   - De la pestaña Body completa
   - Del error que aparece

3. **Revisa los logs del servidor**
   - Busca en la terminal de Quarkus mensajes de error
   - Busca líneas que contengan "400" o "validation"

---

**Fecha**: 6 de enero de 2026  
**Puerto del servidor**: 8080  
**Endpoint**: `/orqcompensacion/v1/transfer`
