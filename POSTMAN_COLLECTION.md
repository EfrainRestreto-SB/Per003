# Guía de Prueba con Postman - PER003 Auditoría

## ✅ Sí, la auditoría funciona con Postman

El sistema de auditoría se ejecuta en el backend automáticamente cuando el endpoint recibe una petición, sin importar el cliente HTTP que uses (Postman, curl, navegador, etc.).

---

## 📋 Configuración en Postman

### 1. Método y URL
```
POST http://localhost:8080/orqcompensacion/v1/transfer
```

### 2. Headers (pestaña Headers)
Agregar estos headers exactamente como se muestran:

| Key | Value |
|-----|-------|
| Content-Type | application/json |
| nombreOperacion | COBPER |
| total | 1 |
| jornada | 20260106 |
| canal | 1 |
| modoDeOperacion | 1 |
| usuario | TESTUSER |
| perfil | 1 |
| versionServicio | 1.0 |
| idTransaccion | test-postman-001 |

### 3. Body (pestaña Body → raw → JSON)
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

## 🔍 Qué Esperar

### ✅ Respuesta Exitosa (Status 200)
La auditoría registrará **2 logs**:

1. **LOG ENTRADA** (al recibir el request)
   ```
   TIPO_MENSAJE: ENTRADA
   PAYLOAD: JSON del TransferCommand
   ID_TRANSACCION: test-postman-001
   ```

2. **LOG SALIDA** (al enviar la response)
   ```
   TIPO_MENSAJE: SALIDA
   PAYLOAD: JSON del TransferResult
   ID_TRANSACCION: test-postman-001
   ```

### ❌ Respuesta con Error (Status 4xx o 5xx)
La auditoría registrará **2 logs**:

1. **LOG ENTRADA** (al recibir el request)
2. **LOG ERROR** (al capturar la excepción)
   ```
   TIPO_MENSAJE: ERROR
   PAYLOAD: JSON de la excepción con stack trace
   ESTADO: ERROR
   DETALLE_ERROR: Mensaje del error
   ```

---

## 🖥️ Verificar los Logs de Auditoría

### Opción 1: Consola de Quarkus
Mira la terminal donde corre `.\mvnw quarkus:dev`. Deberías ver logs como:
```
INFO  Audit - Registrando log de tipo ENTRADA para transacción test-postman-001
INFO  Audit - Registrando log de tipo SALIDA para transacción test-postman-001
```

### Opción 2: Base de Datos DB2
```sql
-- Ver los últimos logs
SELECT ID, ID_TRANSACCION, TIPO_MENSAJE, TIMESTAMP, ESTADO, LOGIN_USER
FROM PERUSRLIB.AUDIT_LOGS
ORDER BY TIMESTAMP DESC
FETCH FIRST 10 ROWS ONLY;

-- Ver logs de una transacción específica
SELECT ID, TIPO_MENSAJE, TIMESTAMP, PAYLOAD, ESTADO, DETALLE_ERROR
FROM PERUSRLIB.AUDIT_LOGS
WHERE ID_TRANSACCION = 'test-postman-001'
ORDER BY TIMESTAMP;
```

---

## 📊 Colección de Postman (JSON)

Puedes importar esta colección en Postman:

```json
{
  "info": {
    "name": "PER003 - Auditoría",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Transfer con Auditoría",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          },
          {
            "key": "nombreOperacion",
            "value": "COBPER"
          },
          {
            "key": "total",
            "value": "1"
          },
          {
            "key": "jornada",
            "value": "20260106"
          },
          {
            "key": "canal",
            "value": "1"
          },
          {
            "key": "modoDeOperacion",
            "value": "1"
          },
          {
            "key": "usuario",
            "value": "TESTUSER"
          },
          {
            "key": "perfil",
            "value": "1"
          },
          {
            "key": "versionServicio",
            "value": "1.0"
          },
          {
            "key": "idTransaccion",
            "value": "{{$randomUUID}}"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"NombredelServicio\": \"TransferenciaService\",\n  \"Data\": {\n    \"codTipoIdentificacion\": \"CC\",\n    \"valNumeroIdentificacion\": \"987654321\",\n    \"codTipoProducto\": \"AHO\",\n    \"valNumeroProducto\": \"1234567890\",\n    \"codMonedaProducto\": \"USD\",\n    \"codTipoMovimiento\": \"DBT\",\n    \"valMonto\": 100.00,\n    \"codMonedaDestino\": \"USD\",\n    \"codTipoConcepto\": \"COBPER\",\n    \"valDescripcion\": \"Prueba de auditoria desde Postman\"\n  }\n}",
          "options": {
            "raw": {
              "language": "json"
            }
          }
        },
        "url": {
          "raw": "http://localhost:8080/orqcompensacion/v1/transfer",
          "protocol": "http",
          "host": [
            "localhost"
          ],
          "port": "8080",
          "path": [
            "orqcompensacion",
            "v1",
            "transfer"
          ]
        }
      }
    }
  ]
}
```

---

## 🚀 Pasos para Probar

1. **Asegúrate que el servidor esté corriendo:**
   ```powershell
   cd "c:\Users\user\Documents\Proyectos\Davivienda\per\Per003\local\PER003"
   .\mvnw quarkus:dev
   ```
   Espera hasta ver: `Listening on: http://localhost:8080`

2. **Abre Postman** y configura la petición según la guía anterior

3. **Haz clic en "Send"**

4. **Verifica los logs** en la consola de Quarkus o en la base de datos

---

## ✨ Características de la Auditoría

- ✅ **Automática**: Se ejecuta sin configuración adicional
- ✅ **Asíncrona**: No bloquea la respuesta al cliente
- ✅ **Independiente**: Usa transacciones separadas (no afecta la transacción de negocio)
- ✅ **Resiliente**: Tiene 3 reintentos con backoff exponencial
- ✅ **Segura**: Calcula SHA-256 del payload para integridad
- ✅ **Universal**: Funciona con cualquier cliente HTTP

---

## 📝 Notas Importantes

1. **La auditoría funciona aunque la tabla AUDIT_LOGS no exista**  
   Los logs se intentarán guardar y si falla (porque la tabla no existe), se loguea el error pero la petición continúa.

2. **ID de Transacción único**  
   Usa un `idTransaccion` diferente en cada prueba para poder identificar los logs fácilmente.

3. **Logs asíncronos**  
   Los logs se guardan en background. La respuesta HTTP se envía inmediatamente sin esperar.

4. **Dos escenarios**  
   - **Exitoso**: ENTRADA + SALIDA  
   - **Con error**: ENTRADA + ERROR

---

**Fecha**: 6 de enero de 2026  
**Versión**: 1.0.0-SNAPSHOT  
**Puerto**: 8080
