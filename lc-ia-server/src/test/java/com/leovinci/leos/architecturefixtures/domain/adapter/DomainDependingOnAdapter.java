package com.leovinci.leos.architecturefixtures.domain.adapter;

import com.leovinci.leos.architecturefixtures.adapters.FakeAdapter;

public class DomainDependingOnAdapter {

    private final FakeAdapter adapter;

    public DomainDependingOnAdapter(FakeAdapter adapter) {
        this.adapter = adapter;
    }
}
