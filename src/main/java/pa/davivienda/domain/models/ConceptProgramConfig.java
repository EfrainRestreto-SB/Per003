package pa.davivienda.domain.models;

/**
 * Modelo que representa la configuración de homologación entre tipo de concepto y programa AS/400.
 * Esta clase se usa en el Map de configuración en memoria, no en base de datos.
 * 
 * <p>Cada instancia define:
 * - El tipo de concepto (COBPER, TRCPRO, etc.)
 * - La clave de perfil de control (cntrlprf.prfkey)
 * - El programa AS/400 a ejecutar (PER001, PER004, etc.)
 * - Si está implementado o pendiente
 */
public class ConceptProgramConfig {

    private final String codTipoConcepto;
    private final String cntrlprfPrfkey;
    private final String descripcion;
    private final String programa;
    private final boolean implementado;

    public ConceptProgramConfig(String codTipoConcepto, String cntrlprfPrfkey, 
                                String descripcion, String programa, boolean implementado) {
        this.codTipoConcepto = codTipoConcepto;
        this.cntrlprfPrfkey = cntrlprfPrfkey;
        this.descripcion = descripcion;
        this.programa = programa;
        this.implementado = implementado;
    }

    public String getCodTipoConcepto() {
        return codTipoConcepto;
    }

    public String getCntrlprfPrfkey() {
        return cntrlprfPrfkey;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getPrograma() {
        return programa;
    }

    public boolean isImplementado() {
        return implementado;
    }

    @Override
    public String toString() {
        return "ConceptProgramConfig{" +
                "codTipoConcepto='" + codTipoConcepto + '\'' +
                ", cntrlprfPrfkey='" + cntrlprfPrfkey + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", programa='" + programa + '\'' +
                ", implementado=" + implementado +
                '}';
    }
}
