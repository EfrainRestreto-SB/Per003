package pa.davivienda.persistence.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import pa.davivienda.domain.interfaces.respositories.Per003Repository;
import pa.davivienda.domain.models.requests.Per003RequestModel;
import pa.davivienda.domain.models.responses.Per003ResponseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementación del repositorio usando StatelessSession de Hibernate
 * para operaciones de alto rendimiento sin caché de primer nivel.
 */
@ApplicationScoped
public class Per003RepositoryImpl implements Per003Repository {

    private static final Logger LOG = LoggerFactory.getLogger(Per003RepositoryImpl.class);

    @Inject
    EntityManager entityManager;

    @Override
    public Per003ResponseModel getMessage(Per003RequestModel request) {
        LOG.info("Ejecutando consulta con StatelessSession");

        // Obtener SessionFactory desde EntityManager
        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        
        try (StatelessSession session = sessionFactory.openStatelessSession()) {
            session.beginTransaction();
            
            try {
                // TODO: Implementar consulta/operación DB2
                // Ejemplo:
                // String query = "SELECT ... FROM ...";
                // Object result = session.createNativeQuery(query, EntityClass.class)
                //     .setParameter("param", request.getParam())
                //     .getSingleResult();
                
                Per003ResponseModel response = new Per003ResponseModel();
                
                session.getTransaction().commit();
                return response;
                
            } catch (Exception e) {
                LOG.error("Error en operación DB2: {}", e.getMessage(), e);
                session.getTransaction().rollback();
                throw e;
            }
        }
    }
}
