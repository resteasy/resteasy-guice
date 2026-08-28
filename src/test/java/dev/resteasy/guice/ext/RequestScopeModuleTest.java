/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice.ext;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import dev.resteasy.guice.RequestScoped;
import dev.resteasy.guice.ResteasyGuiceTestExtension;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;

public class RequestScopeModuleTest {
    @RegisterExtension
    private static final ResteasyGuiceTestExtension TEST_EXTENSION = new ResteasyGuiceTestExtension(TestModule.class,
            JaxrsModule.class, RequestScopeModule.class);

    @Test
    public void testInjection() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(TEST_EXTENSION.getBaseUri()).path("test").request().get()) {
                Assertions.assertEquals(200, response.getStatus(), () -> "Expected a 200 status but got %d: %s"
                        .formatted(response.getStatus(), response.readEntity(String.class)));
                Assertions.assertEquals("ok", response.readEntity(String.class));
            }
        }
    }

    /**
     * Tests the fix for RESTEASY-1428: calling {@code toString()} on the request-scoped
     * provider must not fail outside of a request. Thanks to Antti Lampinen for this test.
     */
    @Test
    public void testToString() {
        final Key<Injector> key = Key.get(Injector.class);
        final Injector injector = Guice.createInjector(new RequestScopeModule());
        final Provider<Injector> unscoped = injector.getProvider(key);
        final Provider<Injector> scoped = injector.getScopeBindings().get(RequestScoped.class).scope(key, unscoped);
        Assertions.assertDoesNotThrow(scoped::toString);
    }

    @Path("test")
    public interface TestResource {
        @GET
        String getName();
    }

    public static class TestModule implements Module {
        @Override
        public void configure(final Binder binder) {
            binder.bind(TestResource.class).to(RequestScopeTestResource.class);
        }
    }

    /**
     * Demonstrates the Guice-native way to obtain the JAX-RS context objects: install
     * {@link RequestScopeModule} and use ordinary Guice constructor injection (plain
     * {@code @Inject}, not {@code @Context}). Constructor injection cannot use {@code @Context}
     * because Guice, not RESTEasy, instantiates the resource; {@code RequestScopeModule} bridges
     * that gap by binding each context type to a request-scoped provider backed by
     * {@code ResteasyContext}.
     */
    public static class RequestScopeTestResource implements TestResource {
        private final Request request;
        private final HttpHeaders httpHeaders;
        private final UriInfo uriInfo;
        private final SecurityContext securityContext;
        private final HttpServletRequest servletRequest;
        private final HttpServletResponse servletResponse;

        @Inject
        public RequestScopeTestResource(final Request request, final HttpHeaders httpHeaders, final UriInfo uriInfo,
                final SecurityContext securityContext, final HttpServletRequest servletRequest,
                final HttpServletResponse servletResponse) {
            this.request = request;
            this.httpHeaders = httpHeaders;
            this.uriInfo = uriInfo;
            this.securityContext = securityContext;
            this.servletRequest = servletRequest;
            this.servletResponse = servletResponse;
        }

        @Override
        public String getName() {
            Assertions.assertNotNull(request);
            Assertions.assertNotNull(httpHeaders);
            Assertions.assertNotNull(uriInfo);
            Assertions.assertNotNull(securityContext);
            Assertions.assertNotNull(servletRequest);
            Assertions.assertNotNull(servletResponse);
            return "ok";
        }
    }
}
