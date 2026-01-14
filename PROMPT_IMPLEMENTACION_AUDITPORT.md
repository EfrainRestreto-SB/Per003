# 📝 PROMPT: Implementación de AuditPort para Microservicio

## 🎯 Objetivo
Implementar un sistema de auditoría completo siguiendo arquitectura hexagonal para registrar trazabilidad funcional de transacciones en un microservicio Quarkus con DB2 i (AS/400).

---

## 📐 Arquitectura Hexagonal

```
┌─────────────────────────────────────────────────────────┐
│                    CAPA APLICACIÓN                      │
│                                                         │
│  ┌──────────────────────────────────────────┐         │
│  │     UseCaseImpl (Lógica de Negocio)     │         │
│  │                                          │         │
│  │  @Inject AuditPort auditPort;           │         │
│  │                                          │         │
│  │  auditPort.logAsync(auditLog);          │         │
│  └──────────────┬───────────────────────────┘         │
└─────────────────┼─────────────────────────────────────┘
                  │
                  │ Depende de (Puerto)
                  ▼
┌─────────────────────────────────────────────────────────┐
│                     CAPA DOMINIO                        │
│                                                         │
│  ┌──────────────────────────────────────────┐         │
│  │        AuditPort (Interface)             │         │
│  │                                          │         │
│  │  void log(AuditLog auditLog);           │         │
│  │  void logAsync(AuditLog auditLog);      │         │
│  └──────────────────────────────────────────┘         │
│                                                         │
│  ┌──────────────────────────────────────────┐         │
│  │        AuditLog (Entity)                 │         │
│  │  AuditMessageType (Enum)                │         │
│  └──────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────┘
                  ▲
                  │ Implementa
                  │
┌─────────────────────────────────────────────────────────┐
│                  CAPA PERSISTENCIA                      │
│                                                         │
│  ┌──────────────────────────────────────────┐         │
│  │   AuditAdapterJdbc (Implementación)     │         │
│  │                                          │         │
│  │  implements AuditPort                   │         │
│  │  @ApplicationScoped                     │         │
│  │                                          │         │
│  │  - StatelessSession                     │         │
│  │  - Retry Logic (3 intentos)            │         │
│  │  - No propaga excepciones               │         │
│  └──────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────┘
```

---

## 📦 Estructura de Paquetes

```
src/main/java/
├── {tuPackage}/
│   ├── domain/
│   │   ├── entities/
│   │   │   └── AuditLog.java                    ✅ Entidad inmutable
│   │   ├── enums/
│   │   │   └── AuditMessageType.java           ✅ ENTRADA|TRAMA_OUT|TRAMA_IN|SALIDA|ERROR
│   │   └── ports/
│   │       └── output/
│   │           └── AuditPort.java               ✅ Interfaz (puerto)
│   │
│   ├── persistence/
│   │   └── adapters/
│   │       └── AuditAdapterJdbc.java           ✅ Implementación con StatelessSession
│   │
│   ├── application/
│   │   └── usecases/
│   │       └── TuUseCaseImpl.java              ✅ Usa @Inject AuditPort
│   │
│   └── transversal/
│       └── utils/
│           └── AuditUtils.java                  ✅ Helper para JSON y SHA-256
│
sql/
└── create_audit_table.sql                        ✅ DDL para DB2 i
```

---

## 🔧 PASO 1: Crear el Enum AuditMessageType

**Ubicación**: `domain/enums/AuditMessageType.java`

```java
package {tuPackage}.domain.enums;

/**
 * Tipos de mensajes de auditoría para el sistema de logging funcional.
 * 
 * Cada transacción debe generar exactamente 4 registros:
 * - ENTRADA: Request recibido desde el BUS
 * - TRAMA_OUT: Mensaje transformado hacia AS/400
 * - TRAMA_IN: Respuesta recibida desde AS/400
 * - SALIDA: Response final enviado al BUS
 * - ERROR: Registro adicional en caso de excepción
 */
public enum AuditMessageType {
    /**
     * Request recibido desde el BUS (headers + body)
     */
    ENTRADA,
    
    /**
     * Mensaje transformado hacia AS/400 (query + parámetros)
     */
    TRAMA_OUT,
    
    /**
     * Respuesta recibida desde AS/400
     */
    TRAMA_IN,
    
    /**
     * Response final enviado al BUS (headers + body)
     */
    SALIDA,
    
    /**
     * Registro de error (excepción + stack trace)
     */
    ERROR
}
```

---

## 🔧 PASO 2: Crear la Entidad AuditLog

**Ubicación**: `domain/entities/AuditLog.java`

```java
package {tuPackage}.domain.entities;

import java.time.Instant;

import {tuPackage}.domain.enums.AuditMessageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Entidad de auditoría para logging funcional.
 * 
 * Representa un registro en la tabla {TU_SCHEMA}.AUDIT_LOGS
 * Inmutable usando Lombok @Builder
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLog {
    
    /**
     * ID de transacción del BUS (viene en headers)
     */
    private String idTransaccion;
    
    /**
     * Tipo de mensaje: ENTRADA, TRAMA_OUT, TRAMA_IN, SALIDA, ERROR
     */
    private AuditMessageType tipoMensaje;
    
    /**
     * CUN del cliente (CUSCUN de tabla CUMST)
     */
    private String logCun;
    
    /**
     * Canal de origen (BM, BI, etc.)
     */
    private String logCanal;
    
    /**
     * Usuario que ejecuta la operación
     */
    private String loginUser;
    
    /**
     * Timestamp del evento
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    /**
     * Payload en formato JSON
     */
    private String payload;
    
    /**
     * Hash SHA-256 del payload
     */
    private String payloadHash;
    
    /**
     * Estado: OK / ERROR
     */
    @Builder.Default
    private String estado = "OK";
    
    /**
     * Detalle del error (si aplica)
     */
    private String detalleError;
    
    /**
     * Origen del microservicio
     */
    @Builder.Default
    private String origen = "{TU_MICROSERVICIO}";  // ⚠️ CAMBIAR
    
    /**
     * Servicio que genera el registro de auditoría
     */
    @Builder.Default
    private String servicio = "{TU_MICROSERVICIO}";  // ⚠️ CAMBIAR
    
    /**
     * Sistema/Usuario creador
     */
    @Builder.Default
    private String createdBy = "{TU_MICROSERVICIO}-SERVICE";  // ⚠️ CAMBIAR
}
```

---

## 🔧 PASO 3: Crear la Interfaz AuditPort

**Ubicación**: `domain/ports/output/AuditPort.java`

```java
package {tuPackage}.domain.ports.output;

import {tuPackage}.domain.entities.AuditLog;

/**
 * Puerto de salida para auditoría (Arquitectura Hexagonal).
 * 
 * Define el contrato para registrar logs de auditoría funcional.
 * La implementación concreta estará en la capa de persistencia.
 * 
 * Reglas:
 * - No debe romper el flujo principal si falla
 * - Debe implementar retry logic (3 intentos)
 * - Transaccionalmente independiente
 */
public interface AuditPort {
    
    /**
     * Registra un log de auditoría de forma síncrona.
     * 
     * Si falla el registro (después de reintentos), NO debe propagar la excepción.
     * Solo debe loguear el error internamente.
     * 
     * @param auditLog Datos del log a registrar
     */
    void log(AuditLog auditLog);
    
    /**
     * Registra un log de auditoría de forma asíncrona (fire-and-forget).
     * 
     * Útil para no bloquear el flujo principal.
     * 
     * @param auditLog Datos del log a registrar
     */
    void logAsync(AuditLog auditLog);
}
```

---

## 🔧 PASO 4: Crear la Clase Utilitaria AuditUtils

**Ubicación**: `transversal/utils/AuditUtils.java`

```java
package {tuPackage}.transversal.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Utilidades para el sistema de auditoría.
 * 
 * Proporciona métodos helper para:
 * - Serialización a JSON
 * - Cálculo de hash SHA-256
 * - Manejo de timestamps
 */
public class AuditUtils {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    
    /**
     * Convierte un objeto a JSON string.
     * 
     * @param obj Objeto a serializar
     * @return JSON string o mensaje de error si falla
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "{}";
        }
        
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"Failed to serialize: " + e.getMessage() + "\"}";
        }
    }
    
    /**
     * Calcula el hash SHA-256 de un string.
     * 
     * @param input String a hashear
     * @return Hash SHA-256 en formato hexadecimal (64 caracteres)
     */
    public static String calculateSHA256(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 siempre está disponible en JDK 17
            return "";
        }
    }
    
    /**
     * Convierte array de bytes a string hexadecimal.
     * 
     * @param bytes Array de bytes
     * @return String hexadecimal
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
    
    /**
     * Crea un JSON con información de una query SQL.
     * 
     * @param queryName Nombre identificador de la query
     * @param query SQL query
     * @param params Parámetros de la query
     * @return JSON string
     */
    public static String queryToJson(String queryName, String query, Object... params) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"queryName\":\"").append(queryName).append("\",");
        json.append("\"query\":\"").append(escapeJson(query)).append("\",");
        json.append("\"params\":[");
        
        for (int i = 0; i < params.length; i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(params[i]).append("\"");
        }
        
        json.append("]}");
        return json.toString();
    }
    
    /**
     * Escapa caracteres especiales para JSON.
     * 
     * @param str String a escapar
     * @return String escapado
     */
    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Crea un JSON con información de una excepción.
     * 
     * @param ex Excepción
     * @param context Contexto donde ocurrió
     * @return JSON string
     */
    public static String exceptionToJson(Exception ex, String context) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"context\":\"").append(context).append("\",");
        json.append("\"exception\":\"").append(ex.getClass().getSimpleName()).append("\",");
        json.append("\"message\":\"").append(escapeJson(ex.getMessage())).append("\",");
        json.append("\"stackTrace\":\"").append(escapeJson(getStackTrace(ex))).append("\"");
        json.append("}");
        return json.toString();
    }
    
    /**
     * Obtiene el stack trace completo de una excepción.
     * 
     * @param ex Excepción
     * @return Stack trace como string
     */
    private static String getStackTrace(Exception ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(ex.toString()).append("\n");
        
        for (StackTraceElement element : ex.getStackTrace()) {
            sb.append("  at ").append(element.toString()).append("\n");
        }
        
        // Limitar a primeras 20 líneas para no saturar DB
        String fullTrace = sb.toString();
        String[] lines = fullTrace.split("\n");
        if (lines.length > 20) {
            StringBuilder limited = new StringBuilder();
            for (int i = 0; i < 20; i++) {
                limited.append(lines[i]).append("\n");
            }
            limited.append("... (truncated)");
            return limited.toString();
        }
        
        return fullTrace;
    }
}
```

---

## 🔧 PASO 5: Crear el Adapter AuditAdapterJdbc

**Ubicación**: `persistence/adapters/AuditAdapterJdbc.java`

```java
package {tuPackage}.persistence.adapters;

import java.sql.Timestamp;
import java.util.concurrent.CompletableFuture;

import {tuPackage}.domain.entities.AuditLog;
import {tuPackage}.domain.ports.output.AuditPort;
import {tuPackage}.transversal.utils.AuditUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Adapter JDBC para auditoría usando StatelessSession de Hibernate.
 * 
 * Implementación del puerto AuditPort siguiendo arquitectura hexagonal.
 * 
 * Características:
 * - Usa StatelessSession (como repositorios stateless)
 * - Retry automático (3 intentos)
 * - Transaccionalmente independiente
 * - No propaga excepciones al flujo principal
 */
@ApplicationScoped
public class AuditAdapterJdbc implements AuditPort {
    
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 100;
    
    @Inject
    EntityManager entityManager;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void log(AuditLog auditLog) {
        try {
            insertWithRetry(auditLog, MAX_RETRIES);
        } catch (Exception e) {
            // No propagar excepción - solo loguear
            Log.errorf("Failed to insert audit log after %d retries: %s", 
                      MAX_RETRIES, e.getMessage());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void logAsync(AuditLog auditLog) {
        CompletableFuture.runAsync(() -> log(auditLog));
    }
    
    /**
     * Inserta el log con retry logic.
     * 
     * @param auditLog Log a insertar
     * @param retriesLeft Intentos restantes
     * @throws Exception Si falla después de todos los reintentos
     */
    private void insertWithRetry(AuditLog auditLog, int retriesLeft) throws Exception {
        try {
            insertAuditLog(auditLog);
        } catch (Exception e) {
            if (retriesLeft > 1) {
                // Calcular delay exponencial
                long delay = INITIAL_RETRY_DELAY_MS * (MAX_RETRIES - retriesLeft + 1);
                
                Log.warnf("Audit insert failed, retrying in %dms. Retries left: %d. Error: %s",
                         delay, retriesLeft - 1, e.getMessage());
                
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                
                insertWithRetry(auditLog, retriesLeft - 1);
            } else {
                // Último intento falló
                throw e;
            }
        }
    }
    
    /**
     * Realiza el INSERT en {TU_SCHEMA}.AUDIT_LOGS usando StatelessSession.
     * 
     * @param auditLog Log a insertar
     */
    private void insertAuditLog(AuditLog auditLog) {
        SessionFactory sf = entityManager
                .unwrap(Session.class)
                .getSessionFactory();
        
        try (StatelessSession ss = sf.openStatelessSession()) {
            
            // Iniciar transacción independiente
            ss.beginTransaction();
            
            try {
                // SQL INSERT
                // ⚠️ CAMBIAR {TU_SCHEMA} por tu schema real
                String sql = """
                    INSERT INTO {TU_SCHEMA}.AUDIT_LOGS (
                        ID_TRANSACCION,
                        TIPO_MENSAJE,
                        LOG_CUN,
                        LOG_CANAL,
                        LOGIN_USER,
                        TS,
                        PAYLOAD,
                        PAYLOAD_HASH,
                        ESTADO,
                        DETALLE_ERROR,
                        ORIGEN,
                        SERVICIO,
                        CREATED_BY
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;
                
                // Calcular hash si no está calculado
                String payloadHash = auditLog.getPayloadHash();
                if (payloadHash == null && auditLog.getPayload() != null) {
                    payloadHash = AuditUtils.calculateSHA256(auditLog.getPayload());
                }
                
                // Ejecutar INSERT
                ss.createNativeMutationQuery(sql)
                    .setParameter(1, auditLog.getIdTransaccion())
                    .setParameter(2, auditLog.getTipoMensaje().name())
                    .setParameter(3, auditLog.getLogCun())
                    .setParameter(4, auditLog.getLogCanal())
                    .setParameter(5, auditLog.getLoginUser())
                    .setParameter(6, Timestamp.from(auditLog.getTimestamp()))
                    .setParameter(7, auditLog.getPayload())
                    .setParameter(8, payloadHash)
                    .setParameter(9, auditLog.getEstado())
                    .setParameter(10, auditLog.getDetalleError())
                    .setParameter(11, auditLog.getOrigen())
                    .setParameter(12, auditLog.getServicio())
                    .setParameter(13, auditLog.getCreatedBy())
                    .executeUpdate();
                
                // Commit transacción
                ss.getTransaction().commit();
                
                Log.debugf("Audit log inserted: type=%s, trx=%s", 
                          auditLog.getTipoMensaje(), auditLog.getIdTransaccion());
                
            } catch (Exception e) {
                // Rollback en caso de error
                if (ss.getTransaction().isActive()) {
                    ss.getTransaction().rollback();
                }
                throw e;
            }
        }
    }
}
```

---

## 🔧 PASO 6: Crear el Script SQL de Creación de Tabla

**Ubicación**: `sql/create_audit_table.sql`

```sql
-- =====================================================
-- Tabla de Auditoría para Microservicio {TU_MICROSERVICIO}
-- Database: DB2 i (AS/400)
-- Library: {TU_SCHEMA}
-- =====================================================

-- Eliminar tabla si existe (solo para desarrollo)
-- DROP TABLE {TU_SCHEMA}.AUDIT_LOGS;

-- Crear tabla de auditoría
CREATE TABLE {TU_SCHEMA}.AUDIT_LOGS (
    -- Primary Key autoincremental
    ID_LOG BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (
        START WITH 1 
        INCREMENT BY 1 
        NO CACHE
    ),
    
    -- Identificación de la transacción
    ID_TRANSACCION VARCHAR(50) NOT NULL,
    
    -- Tipo de mensaje
    TIPO_MENSAJE VARCHAR(20) NOT NULL,
    
    -- Datos del cliente
    LOG_CUN VARCHAR(20),
    
    -- Canal de origen
    LOG_CANAL VARCHAR(10),
    
    -- Usuario que ejecuta
    LOGIN_USER VARCHAR(50),
    
    -- Timestamp del evento
    TS TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Payload en JSON
    PAYLOAD CLOB(10M),
    
    -- Hash SHA-256 del payload
    PAYLOAD_HASH VARCHAR(64),
    
    -- Estado del registro
    ESTADO VARCHAR(10) DEFAULT 'OK',
    
    -- Detalle de error (si aplica)
    DETALLE_ERROR VARCHAR(500),
    
    -- Origen del microservicio
    ORIGEN VARCHAR(20) DEFAULT '{TU_MICROSERVICIO}',
    
    -- Servicio que genera el registro
    SERVICIO VARCHAR(10) DEFAULT '{TU_MICROSERVICIO}',
    
    -- Usuario/Sistema creador
    CREATED_BY VARCHAR(50) DEFAULT '{TU_MICROSERVICIO}-SERVICE',
    
    -- Constraints
    PRIMARY KEY (ID_LOG)
);

-- Crear índices para mejorar performance
CREATE INDEX IDX_AUDIT_ID_TRX ON {TU_SCHEMA}.AUDIT_LOGS(ID_TRANSACCION);
CREATE INDEX IDX_AUDIT_TIPO ON {TU_SCHEMA}.AUDIT_LOGS(TIPO_MENSAJE);
CREATE INDEX IDX_AUDIT_CUN ON {TU_SCHEMA}.AUDIT_LOGS(LOG_CUN);
CREATE INDEX IDX_AUDIT_TS ON {TU_SCHEMA}.AUDIT_LOGS(TS);
CREATE INDEX IDX_AUDIT_ESTADO ON {TU_SCHEMA}.AUDIT_LOGS(ESTADO);

-- Comentarios en columnas
COMMENT ON TABLE {TU_SCHEMA}.AUDIT_LOGS IS 'Tabla de auditoría funcional para microservicio {TU_MICROSERVICIO}';
COMMENT ON COLUMN {TU_SCHEMA}.AUDIT_LOGS.ID_LOG IS 'ID autoincremental del log';
COMMENT ON COLUMN {TU_SCHEMA}.AUDIT_LOGS.ID_TRANSACCION IS 'ID de transacción del BUS';
COMMENT ON COLUMN {TU_SCHEMA}.AUDIT_LOGS.TIPO_MENSAJE IS 'Tipo: ENTRADA|TRAMA_OUT|TRAMA_IN|SALIDA|ERROR';
COMMENT ON COLUMN {TU_SCHEMA}.AUDIT_LOGS.LOG_CUN IS 'CUN del cliente (CUSCUN)';
COMMENT ON COLUMN {TU_SCHEMA}.AUDIT_LOGS.LOG_CANAL IS 'Canal: BM, BI, etc.';
COMMENT ON COLUMN {TU_SCHEMA}.AUDIT_LOGS.PAYLOAD IS 'Contenido en formato JSON';
COMMENT ON COLUMN {TU_SCHEMA}.AUDIT_LOGS.PAYLOAD_HASH IS 'Hash SHA-256 del payload';
COMMENT ON COLUMN {TU_SCHEMA}.AUDIT_LOGS.ESTADO IS 'Estado: OK o ERROR';

-- Verificar la creación
SELECT COUNT(*) AS TOTAL_RECORDS FROM {TU_SCHEMA}.AUDIT_LOGS;
```

---

## 🔧 PASO 7: Uso en un UseCase

**Ejemplo completo de implementación en un caso de uso:**

```java
package {tuPackage}.application.usecases;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import {tuPackage}.domain.dtos.requests.HeadersRequestDto;
import {tuPackage}.domain.dtos.requests.TuRequestDto;
import {tuPackage}.domain.dtos.responses.TuResponseDto;
import {tuPackage}.domain.entities.AuditLog;
import {tuPackage}.domain.enums.AuditMessageType;
import {tuPackage}.domain.ports.output.AuditPort;
import {tuPackage}.transversal.utils.AuditUtils;

@ApplicationScoped
public class TuUseCaseImpl implements TuUseCase {

    @Inject
    AuditPort auditPort;

    @Override
    public TuResponseDto ejecutar(HeadersRequestDto headers, TuRequestDto request) {
        String idTransaccion = headers.getIdTransaccion();
        String canal = String.valueOf(headers.getCanal());
        String cun = null;
        
        try {
            // 📝 AUDITORÍA 1: ENTRADA
            auditEntrada(idTransaccion, canal, headers, request);
            
            // ... Tu lógica de negocio aquí ...
            
            // 📝 AUDITORÍA 2: TRAMA_OUT (Query hacia DB)
            auditTramaOut(idTransaccion, canal, "findSomething", param1, param2);
            
            // ... Ejecutar query ...
            
            // 📝 AUDITORÍA 3: TRAMA_IN (Respuesta de DB)
            auditTramaIn(idTransaccion, canal, cun, "findSomething", resultado);
            
            // ... Construir respuesta ...
            
            // 📝 AUDITORÍA 4: SALIDA
            auditSalida(idTransaccion, canal, cun, response);
            
            return response;
            
        } catch (Exception ex) {
            // 📝 AUDITORÍA 5: ERROR
            auditError(idTransaccion, canal, cun, ex);
            throw ex;
        }
    }
    
    /**
     * Audita el request de entrada (ENTRADA).
     */
    private void auditEntrada(String idTransaccion, String canal, 
                             HeadersRequestDto headers, TuRequestDto request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("headers", headers);
        payload.put("body", request);
        
        String payloadJson = AuditUtils.toJson(payload);
        
        AuditLog log = AuditLog.builder()
                .idTransaccion(idTransaccion)
                .tipoMensaje(AuditMessageType.ENTRADA)
                .logCun(null) // Aún no tenemos el CUN
                .logCanal(canal)
                .loginUser("SYSTEM")
                .payload(payloadJson)
                .payloadHash(AuditUtils.calculateSHA256(payloadJson))
                .estado("OK")
                .build();
        
        auditPort.logAsync(log);
    }
    
    /**
     * Audita una query saliente hacia DB (TRAMA_OUT).
     */
    private void auditTramaOut(String idTransaccion, String canal, 
                              String queryName, Object... params) {
        String query = getQueryByName(queryName);
        String payloadJson = AuditUtils.queryToJson(queryName, query, params);
        
        AuditLog log = AuditLog.builder()
                .idTransaccion(idTransaccion)
                .tipoMensaje(AuditMessageType.TRAMA_OUT)
                .logCun(null)
                .logCanal(canal)
                .loginUser("SYSTEM")
                .payload(payloadJson)
                .payloadHash(AuditUtils.calculateSHA256(payloadJson))
                .estado("OK")
                .build();
        
        auditPort.logAsync(log);
    }
    
    /**
     * Audita una respuesta recibida desde DB (TRAMA_IN).
     */
    private void auditTramaIn(String idTransaccion, String canal, String cun,
                             String queryName, Object result) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("queryName", queryName);
        payload.put("result", result);
        
        String payloadJson = AuditUtils.toJson(payload);
        
        AuditLog log = AuditLog.builder()
                .idTransaccion(idTransaccion)
                .tipoMensaje(AuditMessageType.TRAMA_IN)
                .logCun(cun)
                .logCanal(canal)
                .loginUser("SYSTEM")
                .payload(payloadJson)
                .payloadHash(AuditUtils.calculateSHA256(payloadJson))
                .estado("OK")
                .build();
        
        auditPort.logAsync(log);
    }
    
    /**
     * Audita el response final (SALIDA).
     */
    private void auditSalida(String idTransaccion, String canal, String cun,
                            TuResponseDto response) {
        String payloadJson = AuditUtils.toJson(response);
        
        AuditLog log = AuditLog.builder()
                .idTransaccion(idTransaccion)
                .tipoMensaje(AuditMessageType.SALIDA)
                .logCun(cun)
                .logCanal(canal)
                .loginUser("SYSTEM")
                .payload(payloadJson)
                .payloadHash(AuditUtils.calculateSHA256(payloadJson))
                .estado("OK")
                .build();
        
        auditPort.logAsync(log);
    }
    
    /**
     * Audita un error (ERROR).
     */
    private void auditError(String idTransaccion, String canal, String cun, Exception ex) {
        String payloadJson = AuditUtils.exceptionToJson(ex, "TuUseCase");
        
        AuditLog log = AuditLog.builder()
                .idTransaccion(idTransaccion)
                .tipoMensaje(AuditMessageType.ERROR)
                .logCun(cun)
                .logCanal(canal)
                .loginUser("SYSTEM")
                .payload(payloadJson)
                .payloadHash(AuditUtils.calculateSHA256(payloadJson))
                .estado("ERROR")
                .detalleError(ex.getMessage())
                .build();
        
        auditPort.logAsync(log);
    }
    
    private String getQueryByName(String queryName) {
        return switch (queryName) {
            case "findSomething" -> "SELECT * FROM ...";
            default -> queryName;
        };
    }
}
```

---

## 📋 Dependencias Maven Necesarias

Asegúrate de tener estas dependencias en tu `pom.xml`:

```xml
<dependencies>
    <!-- Quarkus REST + Jackson -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-rest-jackson</artifactId>
    </dependency>
    
    <!-- Hibernate ORM (para StatelessSession) -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-hibernate-orm</artifactId>
    </dependency>
    
    <!-- JDBC Driver para DB2 i (AS/400) -->
    <dependency>
        <groupId>com.ibm.db2</groupId>
        <artifactId>jt400</artifactId>
    </dependency>
    
    <!-- Lombok (para @Builder) -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

---

## ✅ Checklist de Implementación

- [ ] Crear enum `AuditMessageType`
- [ ] Crear entidad `AuditLog` con @Builder
- [ ] Crear interfaz `AuditPort`
- [ ] Crear clase `AuditUtils`
- [ ] Crear adapter `AuditAdapterJdbc`
- [ ] Ejecutar script SQL para crear tabla `AUDIT_LOGS`
- [ ] Inyectar `@Inject AuditPort` en tu UseCase
- [ ] Implementar métodos `auditEntrada()`, `auditTramaOut()`, `auditTramaIn()`, `auditSalida()`, `auditError()`
- [ ] Cambiar `{TU_SCHEMA}`, `{TU_MICROSERVICIO}` y `{tuPackage}` por tus valores reales
- [ ] Probar que se generan los 5 tipos de registros (ENTRADA, TRAMA_OUT, TRAMA_IN, SALIDA, ERROR)
- [ ] Verificar que el retry funciona (desconectar DB temporalmente)
- [ ] Verificar que no se rompe el flujo principal si falla auditoría

---

## 📊 Ejemplo de Registros Generados

Una transacción exitosa generará **4 registros**:

```sql
SELECT ID_TRANSACCION, TIPO_MENSAJE, LOG_CUN, ESTADO, TS
FROM {TU_SCHEMA}.AUDIT_LOGS
WHERE ID_TRANSACCION = 'TRX-2025-001'
ORDER BY TS;
```

**Resultado:**
```
ID_TRANSACCION | TIPO_MENSAJE | LOG_CUN      | ESTADO | TS
---------------|--------------|--------------|--------|------------------------
TRX-2025-001   | ENTRADA      | NULL         | OK     | 2025-01-06 10:00:00.000
TRX-2025-001   | TRAMA_OUT    | NULL         | OK     | 2025-01-06 10:00:00.123
TRX-2025-001   | TRAMA_IN     | 1234567890   | OK     | 2025-01-06 10:00:00.456
TRX-2025-001   | SALIDA       | 1234567890   | OK     | 2025-01-06 10:00:00.789
```

Si ocurre un error, se genera un **5to registro**:
```
TRX-2025-001   | ERROR        | 1234567890   | ERROR  | 2025-01-06 10:00:00.999
```

---

## 🎯 Ventajas de Esta Arquitectura

✅ **Arquitectura Hexagonal**: La lógica de negocio no depende de la implementación de persistencia

✅ **Desacoplamiento**: Puedes cambiar la implementación (e.g., usar MongoDB) sin tocar la lógica de negocio

✅ **Resiliente**: Retry automático (3 intentos) con backoff exponencial

✅ **No invasivo**: Si falla la auditoría, no rompe el flujo principal

✅ **Transaccionalmente independiente**: Usa `StatelessSession` con su propia transacción

✅ **Asíncrono**: `logAsync()` no bloquea el flujo principal

✅ **Trazabilidad completa**: Registra entrada, consultas DB, respuestas y errores

✅ **Integridad**: Calcula SHA-256 del payload para detección de manipulación

---

## 🔍 Queries Útiles para Monitoreo

```sql
-- Ver todos los logs de una transacción
SELECT * FROM {TU_SCHEMA}.AUDIT_LOGS 
WHERE ID_TRANSACCION = 'TRX-2025-001'
ORDER BY TS;

-- Ver logs de error
SELECT * FROM {TU_SCHEMA}.AUDIT_LOGS 
WHERE ESTADO = 'ERROR'
ORDER BY TS DESC;

-- Contar logs por tipo
SELECT TIPO_MENSAJE, COUNT(*) AS TOTAL
FROM {TU_SCHEMA}.AUDIT_LOGS
GROUP BY TIPO_MENSAJE;

-- Logs del día
SELECT * FROM {TU_SCHEMA}.AUDIT_LOGS 
WHERE DATE(TS) = CURRENT_DATE
ORDER BY TS DESC;

-- Ver payloads completos
SELECT ID_TRANSACCION, TIPO_MENSAJE, PAYLOAD
FROM {TU_SCHEMA}.AUDIT_LOGS
WHERE ID_TRANSACCION = 'TRX-2025-001';
```

---

## 🚨 Consideraciones Importantes

⚠️ **Cambiar placeholders**:
- `{tuPackage}` → tu paquete base (e.g., `pa.davivienda`)
- `{TU_SCHEMA}` → tu schema en DB2 i (e.g., `PRESURLIB`)
- `{TU_MICROSERVICIO}` → nombre de tu microservicio (e.g., `PER002`)

⚠️ **Tamaño del payload**: El campo `PAYLOAD` es `CLOB(10M)`. Si tus payloads son muy grandes, considera aumentar el tamaño o implementar compresión.

⚠️ **Stack trace truncado**: En `AuditUtils.exceptionToJson()`, el stack trace se limita a 20 líneas para no saturar la DB.

⚠️ **Performance**: Si la auditoría impacta el rendimiento, considera usar una cola asíncrona (e.g., Kafka, RabbitMQ) en lugar de escribir directamente a DB.

---

## 📚 Referencias

- **Arquitectura Hexagonal**: https://alistair.cockburn.us/hexagonal-architecture/
- **Quarkus Hibernate**: https://quarkus.io/guides/hibernate-orm
- **DB2 i DDL**: https://www.ibm.com/docs/en/i/7.5?topic=sql-create-table

---

## ✨ ¡Listo para Implementar!

Copia este prompt completo y tendrás todo lo necesario para implementar el sistema de auditoría AuditPort en tu microservicio siguiendo las mismas prácticas que PER002.

**Recuerda**: Este sistema está diseñado para ser **resiliente** y **no romper el flujo principal** si algo falla. ¡Prueba desconectando la DB para verificar el retry logic!
