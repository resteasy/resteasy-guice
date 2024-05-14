package dev.resteasy.guice.ext;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.RuntimeDelegate;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ResponseBuilderProvider implements Provider<Response.ResponseBuilder> {
    private final RuntimeDelegate runtimeDelegate;

    @Inject
    public ResponseBuilderProvider(final RuntimeDelegate runtimeDelegate) {
        this.runtimeDelegate = runtimeDelegate;
    }

    public Response.ResponseBuilder get() {
        return runtimeDelegate.createResponseBuilder();
    }
}
