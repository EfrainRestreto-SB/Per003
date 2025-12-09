package org.acme.domain.interfaces.respositories;

import org.acme.domain.models.requests.Per003RequestModel;
import org.acme.domain.models.responses.Per003ResponseModel;

public interface Per003Repository {
    Per003ResponseModel getMessage(Per003RequestModel Per003RequestModel);
}
