package pa.davivienda.persistence.repositories;

import jakarta.enterprise.context.ApplicationScoped;
import pa.davivienda.domain.interfaces.respositories.Per003Repository;
import pa.davivienda.domain.models.requests.Per003RequestModel;
import pa.davivienda.domain.models.responses.Per003ResponseModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class Per003RepositoryImpl implements Per003Repository {

    private static final Logger LOG = LoggerFactory.getLogger(Per003RepositoryImpl.class);

    public Per003ResponseModel getMessage(Per003RequestModel Per003RequestModel) {
        LOG.info("Ejecutando método hacerAlgo");

        return new Per003ResponseModel();
    }
}
