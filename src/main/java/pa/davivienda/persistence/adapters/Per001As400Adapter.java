package pa.davivienda.persistence.adapters;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.ProgramCall;
import com.ibm.as400.access.ProgramParameter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pa.davivienda.application.commands.TransferCommand;
import pa.davivienda.application.results.TransferResult;
import pa.davivienda.domain.ports.output.Per001ServicePort;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Adaptador para invocar el programa RPG PER001 en AS/400.
 * Implementa la conversión de TransferCommand a formato trama AS/400
 * y procesa la respuesta del programa RPG.
 */
@ApplicationScoped
public class Per001As400Adapter implements Per001ServicePort {
    
    private static final Logger LOG = LoggerFactory.getLogger(Per001As400Adapter.class);
    
    // Configuración AS400 desde application.yml
    @ConfigProperty(name = "as400.host")
    String as400Host;
    
    @ConfigProperty(name = "as400.username")
    String as400User;
    
    @ConfigProperty(name = "as400.password")
    String as400Password;
    
    @ConfigProperty(name = "as400.library")
    String library;
    
    @ConfigProperty(name = "as400.program.per001")
    String programName;
    
    // Longitudes de las estructuras
    private static final int INHEADER_LENGTH = 215;
    private static final int INBODY_LENGTH = 50;
    private static final int OUTHEADER_LENGTH = 264; // ACEPTABM(1) + ERROR(8) + MENSAJER(255)
    private static final int OUTBODY_LENGTH = 54;    // ONUMCOM(10) + OMONDEB(15) + OMONEDA(4) + OFECHOR(25)
    
    @Override
    public TransferResult processMembershipPayment(TransferCommand command) {
        LOG.info("Invocando programa RPG PER001 para cobro de membresía - idTransaccion: {}", 
                 command.getIdTransaccion());
        
        AS400 as400 = null;
        try {
            // 1. Conectar a AS/400
            as400 = new AS400(as400Host, as400User, as400Password);
            LOG.debug("Conexión establecida con AS/400: {}", as400Host);
            
            // 2. Construir parámetros de entrada (PINHEADER y PINBODY)
            String pinHeader = buildInHeader(command);
            String pinBody = buildInBody(command);
            
            LOG.debug("PINHEADER ({} chars): {}", pinHeader.length(), pinHeader);
            LOG.debug("PINBODY ({} chars): {}", pinBody.length(), pinBody);
            
            // 3. Configurar llamada al programa
            String qualifiedProgram = "/QSYS.LIB/" + library + ".LIB/" + programName + ".PGM";
            ProgramCall program = new ProgramCall(as400);
            program.setProgram(qualifiedProgram);
            
            // 4. Definir parámetros (entrada/salida según firma del RPG)
            ProgramParameter[] parameters = new ProgramParameter[] {
                new ProgramParameter(pinHeader.getBytes(StandardCharsets.UTF_8)),  // PINHEADER (input)
                new ProgramParameter(pinBody.getBytes(StandardCharsets.UTF_8)),    // PINBODY (input)
                new ProgramParameter(OUTHEADER_LENGTH),                             // POUTHEADER (output)
                new ProgramParameter(OUTBODY_LENGTH)                                // POUTBODY (output)
            };
            program.setParameterList(parameters);
            
            // 5. Ejecutar programa RPG
            LOG.info("Ejecutando programa: {}", qualifiedProgram);
            boolean success = program.run();
            
            if (!success) {
                // Error en la ejecución
                AS400Message[] messageList = program.getMessageList();
                StringBuilder errorMsg = new StringBuilder("Error ejecutando PER001: ");
                for (AS400Message message : messageList) {
                    errorMsg.append(message.getText()).append("; ");
                    LOG.error("AS/400 Message: {} - {}", message.getID(), message.getText());
                }
                throw new RuntimeException(errorMsg.toString());
            }
            
            // 6. Procesar respuesta
            byte[] outHeaderBytes = parameters[2].getOutputData();
            byte[] outBodyBytes = parameters[3].getOutputData();
            
            String outHeader = new String(outHeaderBytes, StandardCharsets.UTF_8).trim();
            String outBody = new String(outBodyBytes, StandardCharsets.UTF_8).trim();
            
            LOG.debug("POUTHEADER: {}", outHeader);
            LOG.debug("POUTBODY: {}", outBody);
            
            // 7. Parsear respuesta y construir TransferResult
            TransferResult result = parseResponse(command, outHeader, outBody);
            
            LOG.info("PER001 ejecutado exitosamente - comprobante: {}", result.getValNumeroComprobante());
            return result;
            
        } catch (Exception e) {
            LOG.error("Error invocando PER001 - idTransaccion: {}", command.getIdTransaccion(), e);
            throw new RuntimeException("Error al invocar programa RPG PER001: " + e.getMessage(), e);
        } finally {
            if (as400 != null) {
                try {
                    as400.disconnectAllServices();
                    LOG.debug("Desconectado de AS/400");
                } catch (Exception e) {
                    LOG.warn("Error al desconectar de AS/400", e);
                }
            }
        }
    }
    
    /**
     * Construye la estructura INHEADER (215 caracteres).
     */
    private String buildInHeader(TransferCommand command) {
        return buildInHeader(
            command.getNombreOperacion(),           // SERVICIO (20)
            command.getIdTransaccion(),             // IDTRX (36)
            command.getIdSesion(),                  // IDSESION (36)
            formatDateTime(OffsetDateTime.now()),   // FECASIS (30)
            String.valueOf(command.getCanal()),     // CANAL (10)
            command.getCodPais(),                   // PAIS (3)
            command.getUsuario(),                   // USUARIO1 (20)
            command.getCodIdioma(),                 // IDIOMA (10)
            "0.0.0.0"                               // IPCLIENTE (50) - ajustar si está disponible
        );
    }
    
    /**
     * Construye INHEADER con la estructura completa (215 caracteres).
     */
    private String buildInHeader(String servicio, String idTrx, String idSesion, 
                                   String fecAsis, String canal, String pais,
                                   String usuario, String idioma, String ipCliente) {
        StringBuilder header = new StringBuilder(INHEADER_LENGTH);
        
        header.append(padRight(servicio, 20));    // SERVICIO (20)
        header.append(padRight(idTrx, 36));       // IDTRX (36)
        header.append(padRight(idSesion, 36));    // IDSESION (36)
        header.append(padRight(fecAsis, 30));     // FECASIS (30)
        header.append(padRight(canal, 10));       // CANAL (10)
        header.append(padRight(pais, 3));         // PAIS (3)
        header.append(padRight(usuario, 20));     // USUARIO1 (20)
        header.append(padRight(idioma, 10));      // IDIOMA (10)
        header.append(padRight(ipCliente, 50));   // IPCLIENTE (50)
        
        return header.toString();
    }
    
    /**
     * Construye la estructura INBODY (50 caracteres).
     */
    private String buildInBody(TransferCommand command) {
        return buildInBody(
            command.getCodTipoIdentificacion(),    // ITipId (4)
            command.getValNumeroIdentificacion(),  // INumId (30)
            command.getCodTipoProducto(),          // ITippRo (4)
            command.getValNumeroProducto()         // ICuenTa (12)
        );
    }
    
    /**
     * Construye INBODY con la estructura completa (50 caracteres).
     */
    private String buildInBody(String iTipId, String iNumId, String iTippRo, String iCuenTa) {
        StringBuilder body = new StringBuilder(INBODY_LENGTH);
        
        body.append(padRight(iTipId, 4));      // ITipId (4)
        body.append(padRight(iNumId, 30));     // INumId (30)
        body.append(padRight(iTippRo, 4));     // ITippRo (4)
        body.append(padRight(iCuenTa, 12));    // ICuenTa (12)
        
        return body.toString();
    }
    
    /**
     * Parsea la respuesta del programa RPG PER001 y construye el TransferResult.
     * Estructura RPG OUTHEADER (264 chars):
     * - ACEPTABM: Char(1) Pos(1)
     * - ERROR: Zoned(8:0) Pos(2)
     * - MENSAJER: Char(255) Pos(10)
     * 
     * Estructura RPG OUTBODY (54 chars):
     * - ONUMCOM: Char(10) Pos(1)  - Número de comprobante
     * - OMONDEB: Char(15) Pos(11) - Monto débito
     * - OMONEDA: Char(4) Pos(26)  - Código moneda
     * - OFECHOR: Char(25) Pos(30) - Fecha/hora
     */
    private TransferResult parseResponse(TransferCommand command, String outHeader, String outBody) {
        TransferResult result = new TransferResult();
        
        // Mapear campos del comando
        result.setNombreOperacion(command.getNombreOperacion());
        result.setTotal(command.getTotal());
        result.setIdTransaccion(command.getIdTransaccion());
        
        // Parsear OUTHEADER (264 caracteres)
        // Pos(1) en RPG = índice 0 en Java
        String aceptaBM = outHeader.substring(0, 1);  // Pos(1), 1 char
        String errorCode = outHeader.substring(1, 9).trim(); // Pos(2), 8 chars
        String mensajeR = outHeader.substring(9, 264).trim(); // Pos(10), 255 chars
        
        LOG.debug("OUTHEADER parsed - ACEPTABM: {}, ERROR: {}, MENSAJER: {}", aceptaBM, errorCode, mensajeR);
        
        result.setCaracterAceptacion(aceptaBM);
        result.setUltimoMensaje((short) 0);
        
        try {
            result.setCodMsgRespuesta(Integer.parseInt(errorCode));
        } catch (NumberFormatException e) {
            LOG.warn("Error parseando código de error: {}", errorCode);
            result.setCodMsgRespuesta(0);
        }
        
        result.setMsgRespuesta(mensajeR.isEmpty() ? "Operación procesada" : mensajeR);
        
        // Parsear OUTBODY (54 caracteres)
        String oNumCom = outBody.substring(0, 10).trim();    // Pos(1), 10 chars
        String oMonDeb = outBody.substring(10, 25).trim();   // Pos(11), 15 chars
        String oMoneda = outBody.substring(25, 29).trim();   // Pos(26), 4 chars
        String oFecHor = outBody.substring(29, 54).trim();   // Pos(30), 25 chars
        
        LOG.debug("OUTBODY parsed - ONUMCOM: {}, OMONDEB: {}, OMONEDA: {}, OFECHOR: {}", 
                  oNumCom, oMonDeb, oMoneda, oFecHor);
        
        result.setValNumeroComprobante(oNumCom);
        
        // Parsear monto (puede venir como string con decimales)
        try {
            result.setValMonto(new BigDecimal(oMonDeb));
        } catch (NumberFormatException e) {
            LOG.warn("Error parseando monto: {}", oMonDeb);
            result.setValMonto(command.getValMonto()); // Usar el del comando
        }
        
        // Parsear fecha/hora (formato AS/400 puede variar)
        try {
            result.setFecHoraMovimiento(OffsetDateTime.parse(oFecHor));
        } catch (Exception e) {
            LOG.warn("Error parseando fecha/hora: {}", oFecHor);
            result.setFecHoraMovimiento(OffsetDateTime.now());
        }
        
        // Generar secuencial basado en comprobante
        try {
            result.setValSecuencial(Long.parseLong(oNumCom.replaceAll("\\D", "")));
        } catch (NumberFormatException e) {
            result.setValSecuencial(System.currentTimeMillis());
        }
        
        result.setCostoDeLaTransaccion(new BigDecimal("2.5")); // TODO: Obtener del RPG si está disponible
        
        return result;
    }
    
    /**
     * Rellena un string con espacios a la derecha hasta alcanzar la longitud especificada.
     * Si el string es más largo, lo trunca.
     */
    private String padRight(String value, int length) {
        if (value == null) {
            value = "";
        }
        if (value.length() >= length) {
            return value.substring(0, length);
        }
        return String.format("%-" + length + "s", value);
    }
    
    /**
     * Formatea un OffsetDateTime al formato esperado por AS/400.
     */
    private String formatDateTime(OffsetDateTime dateTime) {
        // Formato: "2026-01-06T16:00:00.000000" (30 caracteres)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");
        return dateTime.format(formatter);
    }
}
