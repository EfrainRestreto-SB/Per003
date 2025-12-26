# Implementación Pendiente - Orquestación de Compensaciones (PER003)

## Estado Actual

### ✅ Completado
- [x] Estructura base del proyecto Quarkus
- [x] DTOs REST completos según especificación Excel (37 campos entrada, 15 salida)
- [x] Controllers REST con validación Bean Validation
- [x] Headers HTTP implementados (DataHeader en headers)
- [x] MapStruct mappers (DTOs ↔ Commands/Results)
- [x] Hexagonal architecture base (domain, application, webapi, persistence)
- [x] OpenAPI/Swagger documentado
- [x] Configuración DB2
- [x] .gitignore optimizado

### ❌ Pendiente de Implementación

El servicio `OrqCompensacionUsecaseImpl.transfer()` actualmente es un **mock/stub**. Solo devuelve datos simulados.

---

## Tareas Pendientes

### 1. Crear Interfaces de Puertos (Ports) - Adaptadores Outbound

**Ubicación:** `src/main/java/pa/davivienda/domain/interfaces/ports/`

> **NOTA ARQUITECTURA:** PER003 es un **Orquestador/Router**. 
> - **PER001** es un servicio **externo** (AS400) → requiere Port outbound
> - **PER004 y PER005** son **casos de uso internos** del microservicio PER003 → NO requieren ports
> - **DB2** es acceso local vía JDBC → Port para repositorios

- [ ] **Per001ServicePort.java** (Port Outbound - Servicio Externo)
  - Operación: procesarCobroMembresia(TransferCommand command)
  - Propósito: Invocar PER001 (AS400 externo) para cobro de membresía (COBPER)
  - Tecnología destino: AS400 (requiere conversión a trama)
  - Métodos esperados:
    ```java
    TransferResult processMembershipPayment(TransferCommand command);
    ```

- [ ] **ConceptConfigurationPort.java** (Port - Repositorio DB2)
  - Operación: obtenerConfiguracionConcepto(String codTipoConcepto)
  - Propósito: Consultar tabla de configuración de tipos de concepto
  - Tecnología: DB2 vía JDBC (JPA/Hibernate)
  - Métodos esperados:
    ```java
    ConceptConfiguration getConceptConfiguration(String conceptCode);
    boolean isConceptValid(String conceptCode);
    ```

- [ ] **AuditPort.java** (Port - Repositorio DB2)
  - Operaciones: registrarAuditoria(), registrarEvento()
  - Propósito: Auditoría y trazabilidad de operaciones de routing
  - Tecnología: DB2 vía JDBC (JPA/Hibernate) o logging
  - Métodos esperados:
    ```java
    void auditRoutingDecision(TransferCommand command, String targetService);
    void logEvent(EventLog event);
    ```

---

### 2. Implementar Adaptadores (Adapters)

**Ubicación:** `src/main/java/pa/davivienda/persistence/adapters/`

- [ ] **Per001As400Adapter.java** (Adapter Outbound - Servicio Externo)
  - Implementa: Per001ServicePort
  - Tecnología: Cliente AS400 (IBM Toolbox for Java / JTOpen) o MQ/REST bridge
  - Responsabilidad: Convertir TransferCommand a trama AS400 e invocar PER001
  - Notas: Requiere conversión de REST a formato trama AS400
  - Configuración: URL/host AS400, credenciales, biblioteca PERUSRLIB

- [ ] **ConceptConfigurationAdapter.java** (Adapter - Repositorio DB2)
  - Implementa: ConceptConfigurationPort
  - Tecnología: JPA/Hibernate sobre JDBC con DB2
  - Responsabilidad: Consultar tabla de configuración de conceptos vía JDBC
  - Tabla: TBL_CONCEPT_CONFIG (a definir estructura)
  - Notas: Acceso directo a DB2 por JDBC, NO usa tramas

- [ ] **AuditAdapter.java** (Adapter - Repositorio DB2)
  - Implementa: AuditPort
  - Tecnología: JPA/Hibernate sobre JDBC con DB2, o Kafka, o Log aggregator
  - Responsabilidad: Persistir auditoría de decisiones de routing vía JDBC
  - Notas: Acceso directo a DB2 por JDBC, NO usa tramas

---

### 3. Implementar RoutingStrategy

**Ubicación:** `src/main/java/pa/davivienda/application/strategies/`

> **REGLAS DE NEGOCIO SEGÚN REQUERIMIENTO:**
> - **Canal 81** + **COBPER** → **PER001** (AS400 - Cobro Membresía)
> - **Canal 151** + **TRCPRO** → **PER004** (Java - Transferencia Regional Cuentas Propias)
> - **Canal 151** + **TRCTER** → **PER005** (Java - Transferencia Regional Terceros)

- [ ] **TargetService.java (enum)**
  - Valores: PER001 (externo), PER004 (interno), PER005 (interno)
  - Propósito: Identificar caso de uso destino
  ```java
  public enum TargetService {
      PER001("Cobro Membresía - AS400 Externo"),
      PER004("Transferencia Regional Propias - UseCase Interno"),
      PER005("Transferencia Regional Terceros - UseCase Interno");
  }
  ```

- [ ] **TransferRoutingStrategy.java (interface)**
  - Método: `TargetService route(TransferCommand command)`
  - Propósito: Decidir servicio destino según canal + concepto

- [ ] **ChannelConceptRoutingStrategy.java**
  - Implementa: TransferRoutingStrategy
  - Lógica de routing principal:
    ```java
    public TargetService route(TransferCommand command) {
        short canal = command.getCanal();
        String concepto = command.getCodTipoConcepto();
        
        // Regla 1: Canal 81 → PER001 (solo COBPER)
        if (canal == 81) {
            if (!"COBPER".equals(concepto)) {
                throw new InvalidCombinationException(
                    "Canal 81 requiere concepto COBPER");
            }
            return TargetService.PER001;
        }
        
        // Regla 2: Canal 151 → PER004 o PER005
        if (canal == 151) {
            return switch (concepto) {
                case "TRCPRO" -> TargetService.PER004;
                case "TRCTER" -> TargetService.PER005;
                default -> throw new InvalidConceptException(
                    "Concepto " + concepto + " no válido para canal 151");
            };
        }
        
        throw new InvalidChannelException("Canal " + canal + " no soportado");
    }
    ```

---

### 4.1 Implementar Casos de Uso de Negocio (PER004, PER005)

**Ubicación:** `src/main/java/pa/davivienda/application/usecases/`

> **NOTA:** PER004 y PER005 NO son servicios externos, son casos de uso dentro del microservicio PER003

- [ ] **Per004TransferUseCaseImpl.java**
  - Implementa: Per004TransferUseCase interface
  - Responsabilidad: Lógica de negocio para Transferencia Regional Cuentas Propias (TRCPRO)
  - Flujo:
    1. Validaciones específicas TRCPRO
    2. Ejecutar débito cuenta origen
    3. Ejecutar crédito cuenta destino
    4. Generar comprobante
  - Métodos:
    ```java
    TransferResult processRegionalOwnAccountTransfer(TransferCommand command);
    ```

- [ ] **Per005TransferUseCaseImpl.java**
  - Implementa: Per005TransferUseCase interface
  - Responsabilidad: Lógica de negocio para Transferencia Regional a Terceros (TRCTER)
  - Flujo:
    1. Validaciones específicas TRCTER
    2. Validar cuenta tercero
    3. Ejecutar débito cuenta origen
    4. Ejecutar crédito cuenta tercero
    5. Generar comprobante
  - Métodos:
    ```java
    TransferResult processRegionalThirdPartyTransfer(TransferCommand command);
    ```

---

### 5. Implementar Validaciones de Negocio

**Ubicación:** `src/main/java/pa/davivienda/application/validators/`

- [ ] **TransferValidator.java (interface)**
  - Método: `ValidationResult validate(TransferCommand command)`

- [ ] **ConceptValidator.java**
  - Valida que `codTipoConcepto` sea uno permitido según tabla de configuración
  - Valida combinaciones válidas: Canal 81→COBPER, Canal 151→TRCPRO/TRCTER
  - Consulta: ConceptConfigurationPort para validar contra tabla DB2
  ```java
  public ValidationResult validate(TransferCommand command) {
      String concepto = command.getCodTipoConcepto();
      
      // Validar existencia en tabla de configuración
      if (!conceptConfigPort.isConceptValid(concepto)) {
          return ValidationResult.invalid("Concepto no configurado: " + concepto);
      }
      
      return ValidationResult.valid();
  }
  ```

- [ ] **ChannelValidator.java**
  - Valida que canal sea uno soportado (81 o 151 únicamente)
  - Rechaza cualquier otro valor de canal
  ```java
  public ValidationResult validate(TransferCommand command) {
      short canal = command.getCanal();
      
      if (canal != 81 && canal != 151) {
          return ValidationResult.invalid(
              "Canal " + canal + " no soportado. Solo 81 y 151 permitidos");
      }
      
      return ValidationResult.valid();
  }
  ```

- [ ] **ChannelConceptCombinationValidator.java** (CRÍTICO)
  - Valida combinaciones válidas de canal + concepto
  - Regla 1: Canal 81 solo acepta COBPER
  - Regla 2: Canal 151 solo acepta TRCPRO o TRCTER
  ```java
  public ValidationResult validate(TransferCommand command) {
      short canal = command.getCanal();
      String concepto = command.getCodTipoConcepto();
      
      if (canal == 81 && !"COBPER".equals(concepto)) {
          return ValidationResult.invalid(
              "Canal 81 requiere concepto COBPER. Recibido: " + concepto);
      }
      
      if (canal == 151 && 
          !"TRCPRO".equals(concepto) && 
          !"TRCTER".equals(concepto)) {
          return ValidationResult.invalid(
              "Canal 151 requiere TRCPRO o TRCTER. Recibido: " + concepto);
      }
      
      return ValidationResult.valid();
  }
  ```

- [ ] **AmountLimitValidator.java**
  - Valida límites por transacción según perfil/canal
  - Valida límites diarios/mensuales del usuario
  - Valida monto mínimo/máximo según concepto

- [ ] **BusinessHoursValidator.java**
  - Valida jornada (días hábiles vs. fin de semana)
  - Valida horarios permitidos según canal/operación

- [ ] **PermissionValidator.java**
  - Valida que usuario/perfil tenga permisos para la operación
  - Valida que canal permita el tipo de operación

- [ ] **CurrencyValidator.java**
  - Valida monedas soportadas
  - Valida combinaciones de moneda origen/destino
  - Valida necesidad de tipo de cambio

- [ ] **AccountValidator.java** (si aplica)
  - Valida formato de cuentas
  - Valida existencia de cuentas
  - Valida estado de cuentas (activa, bloqueada, etc.)

---

### 6. Orquestar el Flujo en OrqCompensacionUsecaseImpl

**Ubicación:** `src/main/java/pa/davivienda/application/usecases/OrqCompensacionUsecaseImpl.java`

- [ ] **Inyectar dependencias**
  ```java
  @Inject ChannelValidator channelValidator;
  @Inject ConceptValidator conceptValidator;
  @Inject ChannelConceptCombinationValidator combinationValidator;
  @Inject ChannelConceptRoutingStrategy routingStrategy;
  
  // Port externo (AS400)
  @Inject Per001ServicePort per001Service;
  
  // UseCases internos
  @Inject Per004TransferUseCase per004UseCase;
  @Inject Per005TransferUseCase per005UseCase;
  
  @Inject AuditPort auditPort;
  ```

- [ ] **Implementar flujo de orquestación en transfer()**
  ```java
  @Override
  @Transactional
  public TransferResult transfer(TransferCommand command) {
      // 1. VALIDACIONES DE ENTRADA
      channelValidator.validate(command);  // Solo 81 o 151
      conceptValidator.validate(command);  // Existe en tabla config
      combinationValidator.validate(command);  // Combinación válida
      
      // 2. AUDITORÍA ENTRADA
      auditPort.logIncomingRequest(command);
      
      // 3. ROUTING (Decisión basada en canal + concepto)
      TargetService targetService = routingStrategy.route(command);
      log.info("Routing to: {} for canal={}, concepto={}", 
               targetService, command.getCanal(), command.getCodTipoConcepto());
      
      // 4. DELEGACIÓN 
      TransferResult result = switch (targetService) {
          case PER001 -> per001Service.processMembershipPayment(command);  // Externo AS400
          case PER004 -> per004UseCase.processRegionalOwnAccountTransfer(command);  // Interno
          case PER005 -> per005UseCase.processRegionalThirdPartyTransfer(command);  // Interno
      };
      
      // 5. AUDITORÍA SALIDA
      auditPort.logOutgoingResponse(result, targetService);
      
      return result;
  }
  ```
  
  **Flujo simplificado:**
  1. **Validación**: Canal, concepto, combinación canal+concepto
  2. **Routing**: Decidir PER001 (externo) / PER004 (interno) / PER005 (interno)
  3. **Delegación**: 
     - PER001 → Invoca servicio AS400 externo vía port
     - PER004/PER005 → Invoca casos de uso internos del microservicio
  4. **Auditoría**: Registrar decisión y resultado

- [ ] **Agregar @Transactional**
  - Garantizar ACID en operaciones de base de datos
  - Configurar política de rollback

---

### 7. Manejo de Errores y Excepciones

**Ubicación:** `src/main/java/pa/davivienda/domain/exceptions/`

- [ ] **BusinessException.java** - Excepciones base de negocio
- [ ] **InvalidChannelException.java** - Canal no soportado (solo 81 y 151)
- [ ] **InvalidConceptException.java** - Concepto no configurado en tabla
- [ ] **InvalidCombinationException.java** - Combinación canal+concepto inválida
- [ ] **ServiceRoutingException.java** - Error al enrutar
- [ ] **ExternalServiceException.java** - Fallo en servicio externo AS400 (PER001)
- [ ] **ExceptionMapper** - Mapear excepciones a ErrorResponse en controller

---

### 8. Testing

**Ubicación:** `src/test/java/pa/davivienda/`

- [ ] **Unit Tests** - Casos de uso aislados
  - OrqCompensacionUsecaseImplTest (orquestador)
  - Per004TransferUseCaseImplTest (caso uso interno)
  - Per005TransferUseCaseImplTest (caso uso interno)
  - ValidatorsTest
  - StrategiesTest
  - MappersTest

- [ ] **Integration Tests** - Flujo completo
  - TransferIntegrationTest (REST → Orquestador → UseCases internos → AS400)
  - DB2 integration tests

- [ ] **Contract Tests** - Contratos de DTOs
  - Validar contra especificación Excel

---

### 9. Configuración y Propiedades

**Ubicación:** `src/main/resources/application.yml`

- [ ] Configurar conexión AS400 (para Per001Adapter):
  ```yaml
  as400:
    host: as400-server.davivienda.local
    port: 8471
    library: PERUSRLIB
    user: ${AS400_USER}
    password: ${AS400_PASSWORD}
  ```
- [ ] Configurar timeouts de servicio AS400 externo
- [ ] Configurar políticas de retry y circuit breaker (Fault Tolerance) para AS400
- [ ] Configurar tabla de conceptos en DB2
- [ ] Configurar logging levels por paquete

---

## Priorización Sugerida

### Sprint 1 - Infraestructura Base (Routing Core)
1. Crear interfaces de Ports (Solo Per001ServicePort para AS400, ConceptConfigurationPort, AuditPort)
2. Crear interfaces de UseCases internos (Per004TransferUseCase, Per005TransferUseCase)
3. Crear enum TargetService y RoutingStrategy
4. Implementar ChannelConceptRoutingStrategy con reglas exactas
5. Implementar validadores (ChannelValidator, ConceptValidator, ChannelConceptCombinationValidator)
6. Crear excepciones de routing
7. Implementar adaptadores mock/stub para desarrollo

### Sprint 2 - Lógica de Negocio (UseCases Internos)
1. Implementar Per004TransferUseCaseImpl (Transferencia Regional Propias - TRCPRO)
2. Implementar Per005TransferUseCaseImpl (Transferencia Regional Terceros - TRCTER)
3. Implementar lógica de negocio específica para cada caso de uso
4. Agregar validaciones específicas por tipo de transferencia
5. Implementar ConceptConfigurationAdapter (JPA/DB2)
6. Testing unitario de cada caso de uso

### Sprint 3 - Orquestación e Integración AS400
1. Refactorizar OrqCompensacionUsecaseImpl con flujo de orquestación completo
2. Integrar validadores en el flujo
3. Integrar routing strategy
4. Implementar delegación: PER001 (externo), PER004/005 (internos)
5. Implementar Per001As400Adapter (conversión a trama AS400)
6. Configurar conexión AS400 en application.yml
7. Agregar @Transactional y manejo de errores
8. Implementar AuditAdapter (JPA/DB2)

### Sprint 4 - Calidad y Producción
1. Unit tests completos (orquestador + casos de uso)
2. Integration tests end-to-end (REST → Routing → UseCases → AS400)
3. Performance testing
4. Configuración de producción
5. Documentación técnica

---

## Métricas de Completitud

**Actual:** ~30% completado (infraestructura REST/DTOs/Mappers)

**Objetivo:** 100% funcional con lógica de orquestación y routing

**Próximo hito:** Routing Strategy + Validadores (llevaría al 60%)

---

## Reglas de Negocio Confirmadas

### Routing por Canal + Concepto

| Canal | Concepto | Servicio Destino | Descripción |
|-------|----------|------------------|-------------|
| 81    | COBPER   | PER001 (AS400)   | Cobro Membresía |
| 151   | TRCPRO   | PER004 (Java)    | Transferencia Regional Cuentas Propias |
| 151   | TRCTER   | PER005 (Java)    | Transferencia Regional Terceros |

### Validaciones Obligatorias

1. **Canal**: Solo 81 o 151 permitidos
2. **Concepto**: Debe existir en tabla de configuración DB2
3. **Combinación**: Canal 81 solo acepta COBPER, Canal 151 solo acepta TRCPRO/TRCTER

---

## Notas Técnicas

- **Patrón:** Hexagonal Architecture (Ports & Adapters) con **Routing Strategy**
- **Rol de PER003:** **Orquestador/Router** que delega a casos de uso internos o servicio externo
- **Framework:** Quarkus 3.30.1
- **Java:** 21
- **Base de datos:** DB2 vía **JDBC** (JPA/Hibernate sobre JDBC)
- **Validación:** Jakarta Bean Validation
- **Mapeo:** MapStruct
- **OpenAPI:** SmallRye OpenAPI
- **Transaccionalidad:** JTA
- **Integración AS400:** IBM Toolbox for Java / JTOpen (solo PER001 - requiere conversión a trama)

> **IMPORTANTE:** 
> - PER003 se comunica con DB2 exclusivamente por **JDBC** (JPA/Hibernate).
> - **PER004 y PER005 son casos de uso INTERNOS** del microservicio PER003, NO servicios externos.
> - Solo **PER001 (AS400)** es un servicio externo que requiere conversión a formato trama.

---

## Arquitectura de Comunicación

```
┌──────────────────────────────────────────────────────────┐
│           Microservicio PER003                           │
│                                                          │
│  ┌────────────────────────────────────────────────────┐ │
│  │  OrqCompensacionUseCase (Orquestador/Router)      │ │
│  │                                                    │ │
│  │  Validators → RoutingStrategy → Delegación        │ │
│  └────┬─────────────────────┬────────────────────┬───┘ │
│       │                     │                    │     │
│       │ interno             │ interno            │     │
│       ▼                     ▼                    │     │
│  ┌─────────┐          ┌─────────┐               │     │
│  │ PER004  │          │ PER005  │               │     │
│  │UseCase  │          │UseCase  │               │     │
│  │(TRCPRO) │          │(TRCTER) │               │     │
│  └────┬────┘          └────┬────┘               │     │
│       │                    │                    │     │
│       └──────┬─────────────┘                    │     │
│              │ JDBC                             │     │
│              ▼                                  │     │
│         ┌────────┐                              │     │
│         │  DB2   │                              │     │
│         └────────┘                              │     │
└─────────────────────────────────────────────────┼─────┘
                                                  │
                                                  │ Port
                                                  │ Trama AS400
                                                  ▼
                                             ┌─────────┐
                                             │ PER001  │
                                             │ (AS400) │
                                             │ COBPER  │
                                             └─────────┘
```

---

## Arquitectura Simplificada

```
┌─────────────────────────────────────┐
│    Microservicio PER003             │
│                                     │
│  Orquestador                        │
│       ├─► PER004 UseCase (interno)  │  TRCPRO [Canal 151]
│       ├─► PER005 UseCase (interno)  │  TRCTER [Canal 151]
│       └─► DB2 (JDBC)                │  Config + Auditoría
│                                     │
└──────────────┬──────────────────────┘
               │
               │ Trama AS400
               ▼
          ┌─────────┐
          │ PER001  │  COBPER [Canal 81]
          │ AS400   │
          └─────────┘
```

---

## Documentos de Referencia

- `PER_INC1_PER003_OrqCompensacionPER.pdf` - Especificación técnico-funcional
- `Campos de entrada.xlsx` - 37 campos de entrada (incluye canal en headers)
- `Campos de salida.xlsx` - 15 campos de salida
- Especificación REST con headers HTTP
- Conceptos soportados: COBPER (PER001), TRCPRO (PER004), TRCTER (PER005)
- Biblioteca AS400: PERUSRLIB
