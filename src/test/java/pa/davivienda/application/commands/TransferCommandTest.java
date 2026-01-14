package pa.davivienda.application.commands;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para la validación del comando {@link TransferCommand}.
 * 
 * <p>Verifica las anotaciones de Bean Validation y la estructura del comando.</p>
 */
class TransferCommandTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidCommand_HasNoValidationErrors() {
        TransferCommand command = createValidCommand();
        Set<ConstraintViolation<TransferCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "Valid command should have no validation errors");
    }

    @Test
    void testIdTransaccion_CanBeNull() {
        TransferCommand command = createValidCommand();
        command.setIdTransaccion(null);
        
        Set<ConstraintViolation<TransferCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "TransferCommand has no Bean Validation constraints");
    }

    @Test
    void testUsuario_CanBeNull() {
        TransferCommand command = createValidCommand();
        command.setUsuario(null);
        
        Set<ConstraintViolation<TransferCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "TransferCommand has no Bean Validation constraints");
    }

    @Test
    void testValMonto_CanBeNegative() {
        TransferCommand command = createValidCommand();
        command.setValMonto(BigDecimal.valueOf(-100));
        
        Set<ConstraintViolation<TransferCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "TransferCommand has no Bean Validation constraints");
    }

    @Test
    void testValMonto_CanBeZero() {
        TransferCommand command = createValidCommand();
        command.setValMonto(BigDecimal.ZERO);
        
        Set<ConstraintViolation<TransferCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "TransferCommand has no Bean Validation constraints");
    }

    @Test
    void testCodTipoIdentificacion_CanBeNull() {
        TransferCommand command = createValidCommand();
        command.setCodTipoIdentificacion(null);
        
        Set<ConstraintViolation<TransferCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "TransferCommand has no Bean Validation constraints");
    }

    @Test
    void testValNumeroIdentificacion_CanBeNull() {
        TransferCommand command = createValidCommand();
        command.setValNumeroIdentificacion(null);
        
        Set<ConstraintViolation<TransferCommand>> violations = validator.validate(command);
        assertTrue(violations.isEmpty(), "TransferCommand has no Bean Validation constraints");
    }

    @Test
    void testHandlesBigDecimalWithDifferentScales() {
        TransferCommand command1 = createValidCommand();
        command1.setValMonto(new BigDecimal("100.00"));

        TransferCommand command2 = createValidCommand();
        command2.setValMonto(new BigDecimal("100.0"));

        TransferCommand command3 = createValidCommand();
        command3.setValMonto(BigDecimal.valueOf(100));

        Set<ConstraintViolation<TransferCommand>> violations1 = validator.validate(command1);
        Set<ConstraintViolation<TransferCommand>> violations2 = validator.validate(command2);
        Set<ConstraintViolation<TransferCommand>> violations3 = validator.validate(command3);

        assertTrue(violations1.isEmpty(), "BigDecimal with scale 2 should be valid");
        assertTrue(violations2.isEmpty(), "BigDecimal with scale 1 should be valid");
        assertTrue(violations3.isEmpty(), "BigDecimal with scale 0 should be valid");
    }

    @Test
    void testFieldsArePreserved() {
        TransferCommand command = new TransferCommand();
        
        command.setIdTransaccion("TXN-12345");
        command.setUsuario("TESTUSER");
        command.setValMonto(BigDecimal.valueOf(1000));
        command.setCodTipoIdentificacion("CC");
        command.setValNumeroIdentificacion("123456789");
        command.setCodTipoProducto("AHO");
        command.setValNumeroProducto("9876543210");
        
        assertEquals("TXN-12345", command.getIdTransaccion());
        assertEquals("TESTUSER", command.getUsuario());
        assertEquals(BigDecimal.valueOf(1000), command.getValMonto());
        assertEquals("CC", command.getCodTipoIdentificacion());
        assertEquals("123456789", command.getValNumeroIdentificacion());
        assertEquals("AHO", command.getCodTipoProducto());
        assertEquals("9876543210", command.getValNumeroProducto());
    }

    @Test
    void testShortAndIntegerFields() {
        TransferCommand command = createValidCommand();
        
        command.setJornada((short) 1);
        command.setCanal((short) 2);
        command.setTotal(100);
        
        assertEquals((short) 1, command.getJornada());
        assertEquals((short) 2, command.getCanal());
        assertEquals(100, command.getTotal());
    }

    // Helper method
    private TransferCommand createValidCommand() {
        TransferCommand command = new TransferCommand();
        command.setIdTransaccion("test-id-12345");
        command.setUsuario("TESTUSER");
        command.setValMonto(BigDecimal.valueOf(100.00));
        command.setCodTipoIdentificacion("CC");
        command.setValNumeroIdentificacion("987654321");
        command.setCodTipoProducto("AHO");
        command.setValNumeroProducto("1234567890");
        command.setNombreOperacion("COBPER");
        command.setJornada((short) 1);
        command.setCanal((short) 1);
        command.setTotal(1);
        return command;
    }
}
