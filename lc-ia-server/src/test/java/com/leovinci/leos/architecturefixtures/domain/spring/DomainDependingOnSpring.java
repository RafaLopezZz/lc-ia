package com.leovinci.leos.architecturefixtures.domain.spring;

import org.springframework.context.ApplicationEventPublisher;

public class DomainDependingOnSpring {

    private final ApplicationEventPublisher publisher;

    public DomainDependingOnSpring(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

}
