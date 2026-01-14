package pa.davivienda.domain.interfaces.respositories;

import pa.davivienda.domain.models.requests.Per003RequestModel;
import pa.davivienda.domain.models.responses.Per003ResponseModel;

public interface Per003Repository {
    Per003ResponseModel getMessage(Per003RequestModel Per003RequestModel);
}
