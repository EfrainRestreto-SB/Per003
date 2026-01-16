package pa.davivienda.application.usecases;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;
import pa.davivienda.application.validators.ChannelConceptValidator;
import pa.davivienda.domain.entities.AuditLog;
import pa.davivienda.domain.enums.AuditMessageType;
import pa.davivienda.domain.exceptions.InvalidChannelConceptException;
import pa.davivienda.domain.ports.output.AuditPort;
import pa.davivienda.domain.ports.output.Per001ServicePort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link OrqCompensacionUsecaseImpl}.
 * 
 * <p>
 * Esta clase de pruebas valida el comportamiento del caso de uso de orquestación
 * de compensación, incluyendo:
 * </p>
 * <ul>
 *   <li>Validación de canal y concepto</li>
 *   <li>Routing correcto según el tipo de concepto (COBPER → PER001)</li>
 *   <li>Registro de auditoría en los momentos correctos</li>
 *   <li>Manejo de errores y excepciones</li>
 *   <li>Métricas y contadores</li>
 *   <li>Generación de respuestas simuladas para conceptos no implementados</li>
 * </ul>
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitarios - OrqCompensacionUsecaseImpl")
class OrqCompensacionUsecaseImplTest {

    @InjectMocks
    OrqCompensacionUsecaseImpl usecase;

    @Mock
    AuditPort auditPort;

    @Mock
    Per001ServicePort per001Service;

    @Mock
    ChannelConceptValidator channelConceptValidator;

    @Mock
    MeterRegistry meterRegistry;

    @Mock
    Counter mockCounter;
    
    @Mock
    DistributionSummary mockSummary;

    @BeforeEach
    void setUp() {
        // Configurar mocks de métricas con lenient() para evitar errores de unnecessary stubbing
        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(mockCounter);
        lenient().when(meterRegistry.summary(anyString(), any(String[].class))).thenReturn(mockSummary);
    }

    // ========== TESTS CONCEPTO COBPER (PER001) ==========

    @Test
    @DisplayName("Debe procesar COBPER exitosamente a través de PER001")
    void testTransfer_COBPER_Success() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 81);
        TransferResult expectedResult = buildPer001Result();
        
        when(per001Service.processMembershipPayment(command)).thenReturn(expectedResult);
        doNothing().when(channelConceptValidator).validate(command);

        // When
        TransferResult result = usecase.transfer(command);

        // Then
        assertNotNull(result);
        assertEquals(expectedResult.getValNumeroComprobante(), result.getValNumeroComprobante());
        assertEquals(expectedResult.getValSecuencial(), result.getValSecuencial());
        
        // Verificar que se llamó al validador
        verify(channelConceptValidator, times(1)).validate(command);
        
        // Verificar que se llamó a PER001
        verify(per001Service, times(1)).processMembershipPayment(command);
        
        // Verificar auditorías: ENTRADA, TRAMA_OUT, TRAMA_IN, SALIDA
        verify(auditPort, times(4)).logAsync(any(AuditLog.class));
        
        // Verificar métricas
        verify(meterRegistry, atLeastOnce()).counter(eq("per001.calls"), any(String[].class));
        verify(meterRegistry, atLeastOnce()).counter(eq("transfer.success"), any(String[].class));
        verify(meterRegistry, atLeastOnce()).summary(eq("transfer.amount"), any(String[].class));
    }

    @Test
    @DisplayName("Debe registrar auditoría ENTRADA al recibir comando COBPER")
    void testTransfer_COBPER_AuditEntrada() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 81);
        TransferResult per001Result = buildPer001Result();
        
        when(per001Service.processMembershipPayment(command)).thenReturn(per001Result);
        doNothing().when(channelConceptValidator).validate(command);
        
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        usecase.transfer(command);

        // Then
        verify(auditPort, times(4)).logAsync(auditCaptor.capture());
        
        AuditLog entradaLog = auditCaptor.getAllValues().get(0);
        assertEquals(AuditMessageType.ENTRADA, entradaLog.getTipoMensaje());
        assertEquals(command.getIdTransaccion(), entradaLog.getIdTransaccion());
        assertEquals("OK", entradaLog.getEstado());
        assertNotNull(entradaLog.getPayload());
    }

    @Test
    @DisplayName("Debe registrar auditoría TRAMA_OUT antes de llamar a PER001")
    void testTransfer_COBPER_AuditTramaOut() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 81);
        TransferResult per001Result = buildPer001Result();
        
        when(per001Service.processMembershipPayment(command)).thenReturn(per001Result);
        doNothing().when(channelConceptValidator).validate(command);
        
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        usecase.transfer(command);

        // Then
        verify(auditPort, times(4)).logAsync(auditCaptor.capture());
        
        AuditLog tramaOutLog = auditCaptor.getAllValues().get(1);
        assertEquals(AuditMessageType.TRAMA_OUT, tramaOutLog.getTipoMensaje());
        assertEquals(command.getIdTransaccion(), tramaOutLog.getIdTransaccion());
    }

    @Test
    @DisplayName("Debe registrar auditoría TRAMA_IN después de recibir respuesta de PER001")
    void testTransfer_COBPER_AuditTramaIn() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 81);
        TransferResult per001Result = buildPer001Result();
        
        when(per001Service.processMembershipPayment(command)).thenReturn(per001Result);
        doNothing().when(channelConceptValidator).validate(command);
        
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        usecase.transfer(command);

        // Then
        verify(auditPort, times(4)).logAsync(auditCaptor.capture());
        
        AuditLog tramaInLog = auditCaptor.getAllValues().get(2);
        assertEquals(AuditMessageType.TRAMA_IN, tramaInLog.getTipoMensaje());
        assertEquals(command.getIdTransaccion(), tramaInLog.getIdTransaccion());
        assertNotNull(tramaInLog.getPayload());
    }

    @Test
    @DisplayName("Debe registrar auditoría SALIDA al completar transferencia COBPER exitosamente")
    void testTransfer_COBPER_AuditSalida() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 81);
        TransferResult per001Result = buildPer001Result();
        
        when(per001Service.processMembershipPayment(command)).thenReturn(per001Result);
        doNothing().when(channelConceptValidator).validate(command);
        
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        usecase.transfer(command);

        // Then
        verify(auditPort, times(4)).logAsync(auditCaptor.capture());
        
        AuditLog salidaLog = auditCaptor.getAllValues().get(3);
        assertEquals(AuditMessageType.SALIDA, salidaLog.getTipoMensaje());
        assertEquals(command.getIdTransaccion(), salidaLog.getIdTransaccion());
        assertEquals("OK", salidaLog.getEstado());
    }

    // ========== TESTS VALIDACIÓN ==========

    @Test
    @DisplayName("Debe rechazar canal diferente a 81 con mensaje 'En desarrollo'")
    void testTransfer_InvalidChannel_ShouldThrowException() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 151);
        
        doThrow(new InvalidChannelConceptException((short) 151, "COBPER", "En desarrollo"))
            .when(channelConceptValidator).validate(command);

        // When & Then
        InvalidChannelConceptException exception = assertThrows(
            InvalidChannelConceptException.class,
            () -> usecase.transfer(command)
        );
        
        assertTrue(exception.getMessage().contains("En desarrollo"));
        
        // Verificar que NO se llamó a PER001
        verify(per001Service, never()).processMembershipPayment(any());
    }

    @Test
    @DisplayName("Debe rechazar concepto diferente a COBPER en canal 81")
    void testTransfer_Canal81_ConceptoInvalido_ShouldThrowException() {
        // Given
        TransferCommand command = buildTestCommand("TRCPRO", (short) 81);
        
        doThrow(new InvalidChannelConceptException((short) 81, "TRCPRO", 
            "Canal 81 no es válido para el concepto TRCPRO"))
            .when(channelConceptValidator).validate(command);

        // When & Then
        assertThrows(InvalidChannelConceptException.class, () -> usecase.transfer(command));
        
        verify(per001Service, never()).processMembershipPayment(any());
    }

    // ========== TESTS MANEJO DE ERRORES ==========

    @Test
    @DisplayName("Debe registrar auditoría ERROR cuando PER001 lanza excepción")
    void testTransfer_COBPER_Per001ThrowsException_ShouldAuditError() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 81);
        RuntimeException per001Exception = new RuntimeException("Error en AS/400");
        
        when(per001Service.processMembershipPayment(command)).thenThrow(per001Exception);
        doNothing().when(channelConceptValidator).validate(command);
        
        ArgumentCaptor<AuditLog> auditCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When & Then
        assertThrows(RuntimeException.class, () -> usecase.transfer(command));

        // Verificar auditorías: ENTRADA, TRAMA_OUT, ERROR
        verify(auditPort, times(3)).logAsync(auditCaptor.capture());
        
        AuditLog errorLog = auditCaptor.getAllValues().get(2);
        assertEquals(AuditMessageType.ERROR, errorLog.getTipoMensaje());
        assertEquals("ERROR", errorLog.getEstado());
        assertEquals(command.getIdTransaccion(), errorLog.getIdTransaccion());
        assertNotNull(errorLog.getDetalleError());
    }

    @Test
    @DisplayName("Debe incrementar contador de errores cuando ocurre excepción")
    void testTransfer_Error_ShouldIncrementErrorCounter() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 81);
        RuntimeException exception = new RuntimeException("Test error");
        
        when(per001Service.processMembershipPayment(command)).thenThrow(exception);
        doNothing().when(channelConceptValidator).validate(command);

        // When
        try {
            usecase.transfer(command);
        } catch (RuntimeException e) {
            // Expected
        }

        // Then
        verify(meterRegistry, atLeastOnce()).counter(
            eq("transfer.error"), 
            any(String[].class)
        );
    }

    // ========== TESTS RESPUESTAS SIMULADAS ==========

    @Test
    @DisplayName("Debe generar respuesta simulada para conceptos no implementados")
    void testTransfer_ConceptoNoImplementado_ShouldReturnSimulatedResult() {
        // Given - Concepto TRCPRO (no implementado, solo válido en canal 151)
        TransferCommand command = buildTestCommand("TRCPRO", (short) 151);
        doNothing().when(channelConceptValidator).validate(command);

        // When
        TransferResult result = usecase.transfer(command);

        // Then
        assertNotNull(result);
        assertNotNull(result.getValNumeroComprobante());
        assertTrue(result.getValNumeroComprobante().startsWith("COMP-"));
        assertEquals("Transacción exitosa (simulada)", result.getMsgRespuesta());
        assertEquals(command.getValMonto(), result.getValMonto());
        
        // Verificar que NO se llamó a PER001
        verify(per001Service, never()).processMembershipPayment(any());
        
        // Verificar contador de simulados
        verify(meterRegistry, atLeastOnce()).counter(eq("transfer.simulated"), any(String[].class));
    }

    // ========== TESTS MÉTRICAS ==========

    @Test
    @DisplayName("Debe incrementar contador de llamadas PER001 para COBPER")
    void testTransfer_COBPER_ShouldIncrementPer001Counter() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 81);
        TransferResult per001Result = buildPer001Result();
        
        when(per001Service.processMembershipPayment(command)).thenReturn(per001Result);
        doNothing().when(channelConceptValidator).validate(command);

        // When
        usecase.transfer(command);

        // Then
        verify(meterRegistry, atLeastOnce()).counter(
            eq("per001.calls"), 
            eq("concept"), eq("COBPER")
        );
    }

    @Test
    @DisplayName("Debe incrementar contador de transferencias exitosas")
    void testTransfer_Success_ShouldIncrementSuccessCounter() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 81);
        TransferResult per001Result = buildPer001Result();
        
        when(per001Service.processMembershipPayment(command)).thenReturn(per001Result);
        doNothing().when(channelConceptValidator).validate(command);

        // When
        usecase.transfer(command);

        // Then
        verify(meterRegistry, atLeastOnce()).counter(
            eq("transfer.success"), 
            eq("concept"), eq("COBPER")
        );
    }

    @Test
    @DisplayName("Debe registrar monto de la transacción en summary")
    void testTransfer_ShouldRecordAmountInSummary() {
        // Given
        TransferCommand command = buildTestCommand("COBPER", (short) 81);
        command.setValMonto(BigDecimal.valueOf(100.50));
        TransferResult per001Result = buildPer001Result();
        
        when(per001Service.processMembershipPayment(command)).thenReturn(per001Result);
        doNothing().when(channelConceptValidator).validate(command);

        // When
        usecase.transfer(command);

        // Then
        verify(meterRegistry, atLeastOnce()).summary(
            eq("transfer.amount"), 
            eq("concept"), eq("COBPER")
        );
        verify(mockSummary, atLeastOnce()).record(100.50);
    }

    // ========== MÉTODOS AUXILIARES ==========

    private TransferCommand buildTestCommand(String concepto, Short canal) {
        TransferCommand command = new TransferCommand();
        
        // Header
        command.setNombreOperacion("TRN");
        command.setTotal(1);
        command.setIdTransaccion("TXN-" + System.currentTimeMillis());
        command.setCanal(canal);
        command.setCodTipoConcepto(concepto);
        
        // Data
        command.setValNumeroIdentificacion("1234567890");
        command.setUsuario("testuser");
        command.setValMonto(BigDecimal.valueOf(50000.00));
        command.setValTasaCambio(BigDecimal.valueOf(1.0));
        command.setValMontoDestino(BigDecimal.valueOf(50000.00));
        command.setCodMonedaDestino("COP");
        
        return command;
    }

    private TransferResult buildPer001Result() {
        TransferResult result = new TransferResult();
        
        // Header
        result.setNombreOperacion("TRN");
        result.setTotal(1);
        result.setCaracterAceptacion("B");
        result.setIdTransaccion("TXN-12345");
        result.setCodMsgRespuesta(0);
        result.setMsgRespuesta("Transacción exitosa");
        
        // Data
        result.setValNumeroComprobante("987654321");
        result.setValSecuencial(123456789L);
        result.setFecHoraMovimiento(OffsetDateTime.now());
        result.setValMonto(BigDecimal.valueOf(50000.00));
        result.setCostoDeLaTransaccion(BigDecimal.ZERO);
        result.setValTasaCambio(BigDecimal.valueOf(1.0));
        result.setValMontoDestino(BigDecimal.valueOf(50000.00));
        result.setCodMonedaTransaccion("COP");
        
        return result;
    }
}
