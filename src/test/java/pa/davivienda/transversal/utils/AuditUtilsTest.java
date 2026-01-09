package pa.davivienda.transversal.utils;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para la clase utilitaria {@link AuditUtils}.
 * 
 * <p>Verifica serialización JSON, hash SHA-256 y manejo de excepciones.</p>
 */
class AuditUtilsTest {

    @Test
    void testToJson_SerializesObjectCorrectly() {
        TestObject obj = new TestObject("test", 123);
        
        String json = AuditUtils.toJson(obj);
        
        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("123"));
    }

    @Test
    void testToJson_HandlesNullCorrectly() {
        String json = AuditUtils.toJson(null);
        
        assertEquals("{}", json);
    }

    @Test
    void testToJson_HandlesEmptyObjectCorrectly() {
        TestObject obj = new TestObject(null, 0);
        
        String json = AuditUtils.toJson(obj);
        
        assertNotNull(json);
        // Should contain JSON structure
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
    }

    @Test
    void testCalculateSHA256_GeneratesHashCorrectly() {
        String input = "test payload";
        
        String hash = AuditUtils.calculateSHA256(input);
        
        assertNotNull(hash);
        assertEquals(64, hash.length(), "SHA-256 hash should be 64 hex characters");
        assertTrue(hash.matches("[a-f0-9]+"), "Hash should be lowercase hexadecimal");
    }

    @Test
    void testCalculateSHA256_IsDeterministic() {
        String input = "test payload";
        
        String hash1 = AuditUtils.calculateSHA256(input);
        String hash2 = AuditUtils.calculateSHA256(input);
        
        assertEquals(hash1, hash2, "Same input should produce same hash");
    }

    @Test
    void testCalculateSHA256_GeneratesDifferentHashesForDifferentInputs() {
        String input1 = "test payload 1";
        String input2 = "test payload 2";
        
        String hash1 = AuditUtils.calculateSHA256(input1);
        String hash2 = AuditUtils.calculateSHA256(input2);
        
        assertNotEquals(hash1, hash2, "Different inputs should produce different hashes");
    }

    @Test
    void testCalculateSHA256_HandlesEmptyString() {
        String hash = AuditUtils.calculateSHA256("");
        
        assertEquals("", hash, "Empty string should return empty hash");
    }

    @Test
    void testCalculateSHA256_HandlesNull() {
        String hash = AuditUtils.calculateSHA256(null);
        
        assertEquals("", hash, "Null input should return empty hash");
    }

    @Test
    void testExceptionToJson_SerializesExceptionCorrectly() {
        Exception ex = new RuntimeException("Test error");
        
        String json = AuditUtils.exceptionToJson(ex, "test context");
        
        assertNotNull(json);
        assertTrue(json.contains("Test error"), "Should contain exception message");
        assertTrue(json.contains("test context"), "Should contain context");
        assertTrue(json.contains("RuntimeException"), "Should contain exception type");
    }

    @Test
    void testExceptionToJson_IncludesExceptionCause() {
        Exception cause = new IllegalArgumentException("Root cause");
        Exception ex = new RuntimeException("Wrapped error", cause);
        
        String json = AuditUtils.exceptionToJson(ex, "test context");
        
        assertNotNull(json);
        assertTrue(json.contains("Wrapped error"), "Should contain main exception message");
        assertTrue(json.contains("test context"), "Should contain context");
    }

    @Test
    void testExceptionToJson_IncludesStackTrace() {
        Exception ex = new RuntimeException("Test error");
        
        String json = AuditUtils.exceptionToJson(ex, "test context");
        
        assertNotNull(json);
        assertTrue(json.contains("stackTrace"), "Should include stack trace");
    }

    @Test
    void testToJson_HandlesComplexObjectsWithDates() {
        ComplexObject obj = new ComplexObject("test", OffsetDateTime.now());
        
        String json = AuditUtils.toJson(obj);
        
        assertNotNull(json);
        assertTrue(json.contains("test"));
        // OffsetDateTime should be serialized in ISO format (contains 'T')
        assertTrue(json.contains("T"), "Dates should be serialized in ISO format");
    }

    @Test
    void testQueryToJson_CreatesQueryAuditLog() {
        String json = AuditUtils.queryToJson("testQuery", "SELECT * FROM test WHERE id = ?", 123);
        
        assertNotNull(json);
        assertTrue(json.contains("testQuery"));
        assertTrue(json.contains("SELECT * FROM test"));
        assertTrue(json.contains("123"));
    }

    @Test
    void testQueryToJson_HandlesMultipleParams() {
        String json = AuditUtils.queryToJson("multiQuery", "UPDATE test SET a=?, b=? WHERE id=?", 
                                             "value1", 42, null);
        
        assertNotNull(json);
        assertTrue(json.contains("multiQuery"));
        assertTrue(json.contains("value1"));
        assertTrue(json.contains("42"));
        assertTrue(json.contains("null"));
    }

    // Helper test classes
    static class TestObject {
        private String name;
        private int value;

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }

    static class ComplexObject {
        private String name;
        private OffsetDateTime timestamp;

        public ComplexObject(String name, OffsetDateTime timestamp) {
            this.name = name;
            this.timestamp = timestamp;
        }

        public String getName() {
            return name;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }
    }
}
