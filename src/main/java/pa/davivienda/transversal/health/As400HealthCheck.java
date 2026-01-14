package pa.davivienda.transversal.health;

import com.ibm.as400.access.AS400;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Health Check para validar la conectividad con AS/400.
 * Verifica que el sistema remoto esté disponible para procesar transacciones PER001.
 */
@Readiness
@ApplicationScoped
public class As400HealthCheck implements HealthCheck {

    @ConfigProperty(name = "as400.host")
    String as400Host;

    @ConfigProperty(name = "as400.username")
    String as400User;

    @ConfigProperty(name = "as400.password")
    String as400Password;

    @Override
    public HealthCheckResponse call() {
        AS400 system = null;
        try {
            // Intenta conectar con AS/400
            system = new AS400(as400Host, as400User, as400Password);
            
            // Valida que la conexión esté activa
            if (system.isConnected()) {
                return HealthCheckResponse
                        .named("AS/400 connection health check")
                        .up()
                        .withData("host", as400Host)
                        .withData("status", "Connected")
                        .withData("system", "AS/400")
                        .build();
            } else {
                return HealthCheckResponse
                        .named("AS/400 connection health check")
                        .down()
                        .withData("host", as400Host)
                        .withData("status", "Not connected")
                        .withData("system", "AS/400")
                        .build();
            }
        } catch (Exception e) {
            return HealthCheckResponse
                    .named("AS/400 connection health check")
                    .down()
                    .withData("host", as400Host)
                    .withData("error", e.getMessage())
                    .withData("system", "AS/400")
                    .build();
        } finally {
            // Libera recursos de conexión
            if (system != null) {
                try {
                    system.disconnectAllServices();
                } catch (Exception e) {
                    // Ignora errores al desconectar
                }
            }
        }
    }
}
