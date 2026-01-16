package pa.davivienda.application.validators;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.domain.exceptions.InvalidChannelConceptException;
import pa.davivienda.domain.models.ConceptProgramConfig;
import pa.davivienda.transversal.config.ConceptProgramMappingConfig;

/**
 * Test unitarios para {@link ChannelConceptValidator}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChannelConceptValidator - Tests de validación canal-concepto")
class ChannelConceptValidatorTest {
    
    @Mock
    private ConceptProgramMappingConfig mappingConfig;
    
    @InjectMocks
    private ChannelConceptValidator validator;
    
    @BeforeEach
    void setUp() {
        // Mock para COBPER - concepto implementado
        ConceptProgramConfig cobperConfig = new ConceptProgramConfig(
            "COBPER",
            "COBPER",
            "Cobro de membresía",
            "PER001",
            true
        );
        
        // Configurar comportamiento del mock
        lenient().when(mappingConfig.findImplementedConfig("COBPER"))
                .thenReturn(Optional.of(cobperConfig));
        lenient().when(mappingConfig.findImplementedConfig("cobper"))
                .thenReturn(Optional.of(cobperConfig));
        lenient().when(mappingConfig.findImplementedConfig("CobPer"))
                .thenReturn(Optional.of(cobperConfig));
        
        lenient().when(mappingConfig.findImplementedConfig("TRCPRO"))
                .thenReturn(Optional.empty()); // No implementado
        lenient().when(mappingConfig.findImplementedConfig("TRCTER"))
                .thenReturn(Optional.empty()); // No implementado
        lenient().when(mappingConfig.findImplementedConfig("TININD"))
                .thenReturn(Optional.empty()); // No implementado
        
        lenient().when(mappingConfig.getProgramForConcept("COBPER"))
                .thenReturn(Optional.of("PER001"));
    }
    
    @Test
    @DisplayName("Debe aceptar canal 81 + COBPER")
    void testValidCombination_Canal81_COBPER() {
        // Arrange
        TransferCommand command = createCommand(81, "COBPER");
        
        // Act & Assert - No debe lanzar excepción
        assertDoesNotThrow(() -> validator.validate(command));
    }
    
    @Test
    @DisplayName("Debe rechazar canal 151 + TRCPRO (no implementado en desarrollo)")
    void testInvalidCombination_Canal151_TRCPRO() {
        // Arrange
        TransferCommand command = createCommand(151, "TRCPRO");
        
        // Act & Assert
        InvalidChannelConceptException exception = assertThrows(
            InvalidChannelConceptException.class,
            () -> validator.validate(command)
        );
        
        assertEquals((short) 151, exception.getCanal().shortValue());
        assertEquals("TRCPRO", exception.getConcepto());
        assertTrue(exception.getMessage().contains("desarrollo"));
    }
    
    @Test
    @DisplayName("Debe rechazar canal 151 + TRCTER (no implementado en desarrollo)")
    void testInvalidCombination_Canal151_TRCTER() {
        // Arrange
        TransferCommand command = createCommand(151, "TRCTER");
        
        // Act & Assert
        InvalidChannelConceptException exception = assertThrows(
            InvalidChannelConceptException.class,
            () -> validator.validate(command)
        );
        
        assertEquals((short) 151, exception.getCanal().shortValue());
        assertEquals("TRCTER", exception.getConcepto());
    }
    
    @Test
    @DisplayName("Debe rechazar canal 151 + TININD (no implementado en desarrollo)")
    void testInvalidCombination_Canal151_TININD() {
        // Arrange
        TransferCommand command = createCommand(151, "TININD");
        
        // Act & Assert
        assertThrows(InvalidChannelConceptException.class, () -> validator.validate(command));
    }
    
    @Test
    @DisplayName("Debe rechazar canal 81 + TRCPRO (combinación inválida)")
    void testInvalidCombination_Canal81_TRCPRO() {
        // Arrange
        TransferCommand command = createCommand(81, "TRCPRO");
        
        // Act & Assert
        assertThrows(InvalidChannelConceptException.class, () -> validator.validate(command));
    }
    
    @Test
    @DisplayName("Debe rechazar canal null")
    void testInvalidCombination_NullCanal() {
        // Arrange
        TransferCommand command = createCommand(null, "COBPER");
        
        // Act & Assert
        InvalidChannelConceptException exception = assertThrows(
            InvalidChannelConceptException.class,
            () -> validator.validate(command)
        );
        
        assertTrue(exception.getMessage().contains("no puede ser null"));
    }
    
    @Test
    @DisplayName("Debe rechazar concepto null")
    void testInvalidCombination_NullConcepto() {
        // Arrange
        TransferCommand command = createCommand(81, null);
        
        // Act & Assert
        InvalidChannelConceptException exception = assertThrows(
            InvalidChannelConceptException.class,
            () -> validator.validate(command)
        );
        
        assertTrue(exception.getMessage().contains("no puede ser null"));
    }
    
    @Test
    @DisplayName("Debe rechazar concepto vacío")
    void testInvalidCombination_EmptyConcepto() {
        // Arrange
        TransferCommand command = createCommand(81, "");
        
        // Act & Assert
        assertThrows(InvalidChannelConceptException.class, () -> validator.validate(command));
    }
    
    @Test
    @DisplayName("Debe rechazar command null")
    void testInvalidCombination_NullCommand() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> validator.validate(null));
    }
    
    @Test
    @DisplayName("isValid() debe retornar true para canal 81 + COBPER")
    void testIsValid_ValidCombination() {
        // Act
        boolean result = validator.isValid((short) 81, "COBPER");
        
        // Assert
        assertTrue(result);
    }
    
    @Test
    @DisplayName("isValid() debe retornar false para canal 151 + TRCPRO")
    void testIsValid_InvalidCombination() {
        // Act
        boolean result = validator.isValid((short) 151, "TRCPRO");
        
        // Assert
        assertFalse(result);
    }
    
    @Test
    @DisplayName("isValid() debe ser case-insensitive para el concepto")
    void testIsValid_CaseInsensitive() {
        // Act & Assert
        assertTrue(validator.isValid((short) 81, "COBPER"));
        assertTrue(validator.isValid((short) 81, "cobper"));
        assertTrue(validator.isValid((short) 81, "CobPer"));
    }
    
    @Test
    @DisplayName("isValid() debe retornar false para null")
    void testIsValid_Null() {
        // Act & Assert
        assertFalse(validator.isValid(null, "COBPER"));
        assertFalse(validator.isValid((short) 81, null));
        assertFalse(validator.isValid(null, null));
    }
    
    // Helper method
    private TransferCommand createCommand(Integer canal, String concepto) {
        TransferCommand command = new TransferCommand();
        command.setCanal(canal != null ? canal.shortValue() : null);
        command.setCodTipoConcepto(concepto);
        command.setIdTransaccion("TX-TEST-001");
        command.setValMonto(BigDecimal.valueOf(100.00));
        return command;
    }
}
