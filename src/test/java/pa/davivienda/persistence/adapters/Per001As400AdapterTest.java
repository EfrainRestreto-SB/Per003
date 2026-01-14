package pa.davivienda.persistence.adapters;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.AS400Text;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test unitario para Per001As400Adapter.
 * 
 * <p>Valida la integración con AS/400 mediante mocks de las bibliotecas jt400,
 * probando escenarios de éxito, errores, timeouts y fallback del Circuit Breaker.
 * 
 * <p>NO realiza conexiones reales al AS/400 - todos los tests son unitarios
 * y utilizan objetos mock para simular respuestas del sistema.
 * 
 * @author Davivienda
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitarios de Per001As400Adapter")
class Per001As400AdapterTest {

    @InjectMocks
    private Per001As400Adapter adapter;

    @Mock
    private AS400 as400Mock;

    @Mock
    private ProgramCall programCallMock;

    @BeforeEach
    void setUp() {
        // Configurar propiedades de AS/400 inyectadas
        setField(adapter, "as400Host", "10.246.17.67");
        setField(adapter, "as400Username", "TESTUSER");
        setField(adapter, "as400Password", "TESTPWD");
        setField(adapter, "library", "TESTLIB");
        setField(adapter, "programName", "PER001");
    }

    @Test
    @DisplayName("Debe procesar cobro exitoso y retornar comprobante")
    void testProcessMembershipPayment_Success() throws Exception {
        // Arrange
        TransferCommand command = createValidCommand();
        
        try (MockedConstruction<AS400> as400Construction = mockConstruction(AS400.class,
                (mock, context) -> {
                    when(mock.getCcsid()).thenReturn(Integer.valueOf(37));
                    doNothing().when(mock).disconnectAllServices();
                });
             MockedConstruction<ProgramCall> programCallConstruction = mockConstruction(ProgramCall.class,
                (mock, context) -> {
                    when(mock.run()).thenReturn(true);
                    when(mock.getParameterList()).thenReturn(createSuccessParameters());
                })) {
            
            // Act
            TransferResult result = adapter.processMembershipPayment(command);
            
            // Assert
            assertNotNull(result, "Result should not be null");
            assertEquals("S", result.getCaracterAceptacion(), "Should be success");
            assertEquals("COMP123456", result.getValNumeroComprobante().trim(), "Comprobante should match");
            assertEquals(1001L, result.getValSecuencial(), "Secuencial should match");
            assertEquals(0, result.getCodMsgRespuesta(), "Response code should be 0 (success)");
            assertEquals("OPERACION EXITOSA", result.getMsgRespuesta().trim(), "Message should match");
            assertEquals(command.getIdTransaccion(), result.getIdTransaccion(), "Transaction ID should match");
            assertEquals(command.getValMonto(), result.getValMonto(), "Amount should match");
        }
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando ProgramCall falla")
    void testProcessMembershipPayment_ProgramCallFailure() throws Exception {
        // Arrange
        TransferCommand command = createValidCommand();
        AS400Message errorMsg = mock(AS400Message.class);
        when(errorMsg.getID()).thenReturn("MCH3401");
        when(errorMsg.getText()).thenReturn("Cannot resolve to object IBSTAXES");
        
        try (MockedConstruction<AS400> as400Construction = mockConstruction(AS400.class,
                (mock, context) -> {
                    when(mock.getCcsid()).thenReturn(Integer.valueOf(37));
                    doNothing().when(mock).disconnectAllServices();
                });
             MockedConstruction<ProgramCall> programCallConstruction = mockConstruction(ProgramCall.class,
                (mock, context) -> {
                    when(mock.run()).thenReturn(false);
                    when(mock.getMessageList()).thenReturn(new AS400Message[]{errorMsg});
                })) {
            
            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adapter.processMembershipPayment(command),
                    "Should throw RuntimeException on program failure");
            
            assertTrue(exception.getMessage().contains("Error en PER001"),
                    "Exception message should mention PER001");
        }
    }

    @Test
    @DisplayName("Debe manejar respuesta con código de error del AS/400")
    void testProcessMembershipPayment_ErrorResponse() throws Exception {
        // Arrange
        TransferCommand command = createValidCommand();
        
        try (MockedConstruction<AS400> as400Construction = mockConstruction(AS400.class,
                (mock, context) -> {
                    when(mock.getCcsid()).thenReturn(Integer.valueOf(37));
                    doNothing().when(mock).disconnectAllServices();
                });
             MockedConstruction<ProgramCall> programCallConstruction = mockConstruction(ProgramCall.class,
                (mock, context) -> {
                    when(mock.run()).thenReturn(true);
                    when(mock.getParameterList()).thenReturn(createErrorParameters());
                })) {
            
            // Act
            TransferResult result = adapter.processMembershipPayment(command);
            
            // Assert
            assertNotNull(result);
            assertEquals("S", result.getCaracterAceptacion(), "Character should be S (success call)");
            assertEquals(999, result.getCodMsgRespuesta(), "Should have error code 999");
            assertTrue(result.getMsgRespuesta().contains("FONDOS INSUFICIENTES"),
                    "Should contain error message");
        }
    }

    @Test
    @DisplayName("Debe ejecutar fallback cuando Circuit Breaker está abierto")
    void testFallbackMembershipPayment() {
        // Arrange
        TransferCommand command = createValidCommand();
        
        // Act
        TransferResult result = adapter.fallbackMembershipPayment(command);
        
        // Assert
        assertNotNull(result, "Fallback result should not be null");
        assertEquals("E", result.getCaracterAceptacion(), "Should indicate error");
        assertEquals(503, result.getCodMsgRespuesta(), "Should return 503 Service Unavailable");
        assertTrue(result.getMsgRespuesta().contains("Circuit Breaker"),
                "Message should mention Circuit Breaker");
        assertTrue(result.getValNumeroComprobante().startsWith("FB-"),
                "Should generate fallback comprobante with FB- prefix");
        assertEquals(0L, result.getValSecuencial(), "Secuencial should be 0");
        assertEquals(command.getIdTransaccion(), result.getIdTransaccion(),
                "Should preserve transaction ID");
    }

    @Test
    @DisplayName("Debe desconectar AS/400 incluso cuando ocurre excepción")
    void testProcessMembershipPayment_DisconnectsOnException() throws Exception {
        // Arrange
        TransferCommand command = createValidCommand();
        
        try (MockedConstruction<AS400> as400Construction = mockConstruction(AS400.class,
                (mock, context) -> {
                    when(mock.getCcsid()).thenReturn(Integer.valueOf(37));
                    doNothing().when(mock).disconnectAllServices();
                });
             MockedConstruction<ProgramCall> programCallConstruction = mockConstruction(ProgramCall.class,
                (mock, context) -> {
                    when(mock.run()).thenThrow(new RuntimeException("Connection timeout"));
                })) {
            
            // Act & Assert
            assertThrows(RuntimeException.class,
                    () -> adapter.processMembershipPayment(command));
            
            // Verify disconnectAllServices was called in finally block
            AS400 as400Instance = as400Construction.constructed().get(0);
            verify(as400Instance, times(1)).disconnectAllServices();
        }
    }

    @Test
    @DisplayName("Debe manejar parámetros null en comando")
    void testProcessMembershipPayment_HandlesNullParameters() throws Exception {
        // Arrange
        TransferCommand command = new TransferCommand();
        command.setIdTransaccion("TXN-NULL-001");
        command.setNombreOperacion("OrqCompensacion");
        command.setTotal((short) 1);
        // Dejar otros campos null para probar nvl() y defaults
        
        try (MockedConstruction<AS400> as400Construction = mockConstruction(AS400.class,
                (mock, context) -> {
                    when(mock.getCcsid()).thenReturn(Integer.valueOf(37));
                    doNothing().when(mock).disconnectAllServices();
                });
             MockedConstruction<ProgramCall> programCallConstruction = mockConstruction(ProgramCall.class,
                (mock, context) -> {
                    when(mock.run()).thenReturn(true);
                    when(mock.getParameterList()).thenReturn(createSuccessParameters());
                })) {
            
            // Act
            TransferResult result = adapter.processMembershipPayment(command);
            
            // Assert
            assertNotNull(result, "Should handle null parameters gracefully");
            assertEquals("S", result.getCaracterAceptacion());
        }
    }

    @Test
    @DisplayName("Debe parsear secuencial correctamente")
    void testParseResponse_SecuencialParsing() throws Exception {
        // Arrange
        TransferCommand command = createValidCommand();
        
        try (MockedConstruction<AS400> as400Construction = mockConstruction(AS400.class,
                (mock, context) -> {
                    when(mock.getCcsid()).thenReturn(Integer.valueOf(37));
                    doNothing().when(mock).disconnectAllServices();
                });
             MockedConstruction<ProgramCall> programCallConstruction = mockConstruction(ProgramCall.class,
                (mock, context) -> {
                    when(mock.run()).thenReturn(true);
                    when(mock.getParameterList()).thenReturn(createParametersWithSecuencial("0000009999"));
                })) {
            
            // Act
            TransferResult result = adapter.processMembershipPayment(command);
            
            // Assert
            assertEquals(9999L, result.getValSecuencial(), "Should parse secuencial correctly");
        }
    }

    @Test
    @DisplayName("Debe manejar secuencial inválido sin fallar")
    void testParseResponse_InvalidSecuencial() throws Exception {
        // Arrange
        TransferCommand command = createValidCommand();
        
        try (MockedConstruction<AS400> as400Construction = mockConstruction(AS400.class,
                (mock, context) -> {
                    when(mock.getCcsid()).thenReturn(Integer.valueOf(37));
                    doNothing().when(mock).disconnectAllServices();
                });
             MockedConstruction<ProgramCall> programCallConstruction = mockConstruction(ProgramCall.class,
                (mock, context) -> {
                    when(mock.run()).thenReturn(true);
                    when(mock.getParameterList()).thenReturn(createParametersWithSecuencial("INVALID"));
                })) {
            
            // Act
            TransferResult result = adapter.processMembershipPayment(command);
            
            // Assert
            assertEquals(0L, result.getValSecuencial(),
                    "Should default to 0 when secuencial is invalid");
        }
    }

    @Test
    @DisplayName("Debe preservar monto original del comando")
    void testProcessMembershipPayment_PreservesAmount() throws Exception {
        // Arrange
        TransferCommand command = createValidCommand();
        BigDecimal originalAmount = new BigDecimal("1500.75");
        command.setValMonto(originalAmount);
        
        try (MockedConstruction<AS400> as400Construction = mockConstruction(AS400.class,
                (mock, context) -> {
                    when(mock.getCcsid()).thenReturn(Integer.valueOf(37));
                    doNothing().when(mock).disconnectAllServices();
                });
             MockedConstruction<ProgramCall> programCallConstruction = mockConstruction(ProgramCall.class,
                (mock, context) -> {
                    when(mock.run()).thenReturn(true);
                    when(mock.getParameterList()).thenReturn(createSuccessParameters());
                })) {
            
            // Act
            TransferResult result = adapter.processMembershipPayment(command);
            
            // Assert
            assertEquals(originalAmount, result.getValMonto(),
                    "Result should preserve original amount");
        }
    }

    @Test
    @DisplayName("Debe manejar múltiples mensajes de error del AS/400")
    void testGetAs400ErrorMessage_MultipleMessages() throws Exception {
        // Arrange
        TransferCommand command = createValidCommand();
        AS400Message msg1 = mock(AS400Message.class);
        AS400Message msg2 = mock(AS400Message.class);
        when(msg1.getID()).thenReturn("CPF0001");
        when(msg1.getText()).thenReturn("Error en archivo");
        when(msg2.getID()).thenReturn("CPF0002");
        when(msg2.getText()).thenReturn("Registro no encontrado");
        
        try (MockedConstruction<AS400> as400Construction = mockConstruction(AS400.class,
                (mock, context) -> {
                    when(mock.getCcsid()).thenReturn(Integer.valueOf(37));
                    doNothing().when(mock).disconnectAllServices();
                });
             MockedConstruction<ProgramCall> programCallConstruction = mockConstruction(ProgramCall.class,
                (mock, context) -> {
                    when(mock.run()).thenReturn(false);
                    when(mock.getMessageList()).thenReturn(new AS400Message[]{msg1, msg2});
                })) {
            
            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> adapter.processMembershipPayment(command));
            
            String errorMsg = exception.getMessage();
            assertTrue(errorMsg.contains("CPF0001") || errorMsg.contains("CPF0002"),
                    "Should include error codes from AS/400 messages");
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Crea un comando válido para testing.
     */
    private TransferCommand createValidCommand() {
        TransferCommand command = new TransferCommand();
        command.setIdTransaccion("TXN-TEST-001");
        command.setNombreOperacion("OrqCompensacion");
        command.setTotal((short) 1);
        command.setCodTipoIdentificacion("CC");
        command.setValNumeroIdentificacion("123456789");
        command.setCodTipoProducto("AH");
        command.setValNumeroProducto("987654321");
        command.setValMonto(new BigDecimal("1000.00"));
        command.setCodMonedaProducto("COP");
        command.setCodTipoConcepto("COBPER");
        command.setValTasaCambio(BigDecimal.ONE);
        command.setValMontoDestino(new BigDecimal("1000.00"));
        return command;
    }

    /**
     * Crea parámetros simulando una respuesta exitosa del AS/400.
     */
    private ProgramParameter[] createSuccessParameters() {
        AS400Text text20 = new AS400Text(20, 37);
        AS400Text text10 = new AS400Text(10, 37);
        AS400Text text5 = new AS400Text(5, 37);
        AS400Text text100 = new AS400Text(100, 37);

        ProgramParameter[] params = new ProgramParameter[11];
        
        // Parámetros de entrada (índices 0-6) - no necesitan mock de salida
        for (int i = 0; i < 7; i++) {
            params[i] = mock(ProgramParameter.class);
        }
        
        // Parámetros de salida (índices 7-10)
        params[7] = mock(ProgramParameter.class);
        when(params[7].getOutputData()).thenReturn(text20.toBytes("COMP123456          "));
        
        params[8] = mock(ProgramParameter.class);
        when(params[8].getOutputData()).thenReturn(text10.toBytes("0000001001"));
        
        params[9] = mock(ProgramParameter.class);
        when(params[9].getOutputData()).thenReturn(text5.toBytes("0    "));
        
        params[10] = mock(ProgramParameter.class);
        when(params[10].getOutputData()).thenReturn(text100.toBytes(padRight("OPERACION EXITOSA", 100)));
        
        return params;
    }

    /**
     * Crea parámetros simulando una respuesta de error del AS/400.
     */
    private ProgramParameter[] createErrorParameters() {
        AS400Text text20 = new AS400Text(20, 37);
        AS400Text text10 = new AS400Text(10, 37);
        AS400Text text5 = new AS400Text(5, 37);
        AS400Text text100 = new AS400Text(100, 37);

        ProgramParameter[] params = new ProgramParameter[11];
        
        for (int i = 0; i < 7; i++) {
            params[i] = mock(ProgramParameter.class);
        }
        
        params[7] = mock(ProgramParameter.class);
        when(params[7].getOutputData()).thenReturn(text20.toBytes("                    "));
        
        params[8] = mock(ProgramParameter.class);
        when(params[8].getOutputData()).thenReturn(text10.toBytes("0000000000"));
        
        params[9] = mock(ProgramParameter.class);
        when(params[9].getOutputData()).thenReturn(text5.toBytes("999  "));
        
        params[10] = mock(ProgramParameter.class);
        when(params[10].getOutputData()).thenReturn(text100.toBytes(padRight("FONDOS INSUFICIENTES", 100)));
        
        return params;
    }

    /**
     * Crea parámetros con un secuencial específico.
     */
    private ProgramParameter[] createParametersWithSecuencial(String secuencial) {
        AS400Text text20 = new AS400Text(20, 37);
        AS400Text text10 = new AS400Text(10, 37);
        AS400Text text5 = new AS400Text(5, 37);
        AS400Text text100 = new AS400Text(100, 37);

        ProgramParameter[] params = new ProgramParameter[11];
        
        for (int i = 0; i < 7; i++) {
            params[i] = mock(ProgramParameter.class);
        }
        
        params[7] = mock(ProgramParameter.class);
        when(params[7].getOutputData()).thenReturn(text20.toBytes("COMP999999          "));
        
        params[8] = mock(ProgramParameter.class);
        when(params[8].getOutputData()).thenReturn(text10.toBytes(padRight(secuencial, 10)));
        
        params[9] = mock(ProgramParameter.class);
        when(params[9].getOutputData()).thenReturn(text5.toBytes("0    "));
        
        params[10] = mock(ProgramParameter.class);
        when(params[10].getOutputData()).thenReturn(text100.toBytes(padRight("OK", 100)));
        
        return params;
    }

    /**
     * Rellena string con espacios a la derecha.
     */
    private String padRight(String str, int length) {
        if (str.length() >= length) {
            return str.substring(0, length);
        }
        return String.format("%-" + length + "s", str);
    }

    /**
     * Inyecta valor en campo privado usando reflection.
     */
    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}
