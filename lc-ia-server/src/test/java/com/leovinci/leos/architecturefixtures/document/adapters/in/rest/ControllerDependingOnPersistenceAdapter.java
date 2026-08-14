package com.leovinci.leos.architecturefixtures.document.adapters.in.rest;

import com.leovinci.leos.architecturefixtures.document.adapters.out.persistence.FakePersistenceAdapter;

public class ControllerDependingOnPersistenceAdapter {

    private final FakePersistenceAdapter persistenceAdapter;

    public ControllerDependingOnPersistenceAdapter(
            FakePersistenceAdapter persistenceAdapter) {
        this.persistenceAdapter = persistenceAdapter;
    }
}
