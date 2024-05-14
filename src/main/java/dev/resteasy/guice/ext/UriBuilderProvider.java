package dev.resteasy.guice.ext;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class UriBuilderProvider implements Provider<UriBuilder> {
    private final RuntimeDelegate runtimeDelegate;

    @Inject
    public UriBuilderProvider(final RuntimeDelegate runtimeDelegate) {
        this.runtimeDelegate = runtimeDelegate;
    }

    public UriBuilder get() {
        return runtimeDelegate.createUriBuilder();
    }
}
