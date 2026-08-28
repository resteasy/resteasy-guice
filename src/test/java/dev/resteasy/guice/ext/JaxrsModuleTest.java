/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice.ext;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.Variant;
import jakarta.ws.rs.ext.RuntimeDelegate;

import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import dev.resteasy.guice.ResteasyGuiceTestExtension;

import com.google.inject.Binder;
import com.google.inject.Module;

public class JaxrsModuleTest {
    @RegisterExtension
    private static final ResteasyGuiceTestExtension TEST_EXTENSION = new ResteasyGuiceTestExtension(TestModule.class,
            JaxrsModule.class);

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

    @Path("test")
    public interface TestResource {
        @GET
        String getName();
    }

    public static class TestModule implements Module {
        @Override
        public void configure(final Binder binder) {
            binder.bind(TestResource.class).to(JaxrsTestResource.class);
        }
    }

    public static class JaxrsTestResource implements TestResource {
        private final ClientHttpEngine clientExecutor;
        private final RuntimeDelegate runtimeDelegate;
        private final Response.ResponseBuilder responseBuilder;
        private final UriBuilder uriBuilder;
        private final Variant.VariantListBuilder variantListBuilder;

        @Inject
        public JaxrsTestResource(final ClientHttpEngine clientExecutor, final RuntimeDelegate runtimeDelegate,
                final Response.ResponseBuilder responseBuilder, final UriBuilder uriBuilder,
                final Variant.VariantListBuilder variantListBuilder) {
            this.clientExecutor = clientExecutor;
            this.runtimeDelegate = runtimeDelegate;
            this.responseBuilder = responseBuilder;
            this.uriBuilder = uriBuilder;
            this.variantListBuilder = variantListBuilder;
        }

        @Override
        public String getName() {
            Assertions.assertNotNull(clientExecutor);
            Assertions.assertNotNull(runtimeDelegate);
            Assertions.assertNotNull(responseBuilder);
            Assertions.assertNotNull(uriBuilder);
            Assertions.assertNotNull(variantListBuilder);
            return "ok";
        }
    }
}
