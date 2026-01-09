package pa.davivienda.transversal.health;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Health Check para validar la conectividad con la base de datos DB2.
 * Implementa verificación de liveness (conexión activa) y readiness (BD operativa).
 */
@Liveness
@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    @Inject
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try {
            // Intenta obtener una conexión de la base de datos
            try (Connection connection = dataSource.getConnection()) {
                // Verifica que la conexión esté activa
                if (connection.isValid(2)) {
                    return HealthCheckResponse
                            .named("Database connection health check")
                            .up()
                            .withData("database", "DB2")
                            .withData("status", "Connection active")
                            .build();
                } else {
                    return HealthCheckResponse
                            .named("Database connection health check")
                            .down()
                            .withData("database", "DB2")
                            .withData("status", "Connection invalid")
                            .build();
                }
            }
        } catch (SQLException e) {
            return HealthCheckResponse
                    .named("Database connection health check")
                    .down()
                    .withData("database", "DB2")
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
