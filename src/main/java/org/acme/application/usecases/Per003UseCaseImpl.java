package org.acme.application.usecases;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.domain.interfaces.respositories.Per003Repository;
import org.acme.domain.interfaces.usecases.Per003UseCase;
import org.acme.domain.models.requests.Per003RequestModel;
import org.acme.domain.models.responses.Per003ResponseModel;

@ApplicationScoped
public class Per003UseCaseImpl implements Per003UseCase {

    @Inject
    private Per003Repository Per003Repository;

    public Per003ResponseModel getMessage(Per003RequestModel Per003RequestModel) {
        return Per003Repository.getMessage(Per003RequestModel);
    }
}
