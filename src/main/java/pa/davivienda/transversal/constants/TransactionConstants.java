package pa.davivienda.transversal.constants;

/**
 * Constantes para transacciones y operaciones del sistema.
 * Centraliza valores literales para mejorar el mantenimiento y evitar magic strings.
 * 
 * @author Davivienda
 * @version 1.0
 * @since 1.0
 */
public final class TransactionConstants {

    private TransactionConstants() {
        throw new AssertionError("No se permite instanciar esta clase de constantes");
    }

    /**
     * Constantes para caracteres de aceptación en respuestas.
     */
    public static final class AcceptanceCode {
        private AcceptanceCode() {}
        
        /** Carácter de aceptación para transacciones exitosas (Bueno) */
        public static final String SUCCESS = "B";
        
        /** Carácter de aceptación para transacciones con error (Malo) */
        public static final String ERROR = "M";
    }

    /**
     * Constantes para tipos de concepto de transacción.
     */
    public static final class ConceptType {
        private ConceptType() {}
        
        /** Concepto: Cobro de Membresía PER */
        public static final String COBPER = "COBPER";
        
        /** Concepto: Transferencia Regional Cuentas Propias */
        public static final String TRCPRO = "TRCPRO";
        
        /** Concepto: Transferencia Regional a Terceros */
        public static final String TRCTER = "TRCTER";
        
        /** Concepto: Transferencia Internacional Individual */
        public static final String TININD = "TININD";
    }

    /**
     * Constantes para nombres de métricas.
     */
    public static final class MetricNames {
        private MetricNames() {}
        
        /** Contador de llamadas a PER001 */
        public static final String PER001_CALLS = "per001.calls";
        
        /** Contador de transferencias exitosas */
        public static final String TRANSFER_SUCCESS = "transfer.success";
        
        /** Contador de errores en transferencias */
        public static final String TRANSFER_ERROR = "transfer.error";
        
        /** Summary de montos de transferencias */
        public static final String TRANSFER_AMOUNT = "transfer.amount";
    }

    /**
     * Constantes para tags de métricas.
     */
    public static final class MetricTags {
        private MetricTags() {}
        
        /** Tag para tipo de concepto */
        public static final String CONCEPT = "concept";
        
        /** Tag para canal */
        public static final String CHANNEL = "channel";
    }
}
