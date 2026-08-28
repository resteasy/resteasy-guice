/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.inject.AbstractModule;

/**
 * A minimal, end-to-end example of using RESTEasy with Guice: a Guice {@link com.google.inject.Module}
 * binds a {@code @Path} resource and its collaborator, and a request is served over HTTP. It exercises
 * {@link GuiceResteasyBootstrapServletContextListener} the way a real deployment does — an embedded servlet
 * container ({@code web.xml} equivalent) with the listener and the RESTEasy dispatcher servlet, driven only
 * by the {@code resteasy.guice.modules} context-param.
 */
public class GuiceServletListenerTest {
    @RegisterExtension
    private static final ResteasyGuiceTestExtension TEST_EXTENSION = new ResteasyGuiceTestExtension(GreetingModule.class);

    @Test
    public void resourceRegisteredAndInjectedThroughListener() {
        try (Client client = ClientBuilder.newClient()) {
            final String result = client.target(TEST_EXTENSION.getBaseUri())
                    .path("greeting")
                    .request(MediaType.TEXT_PLAIN)
                    .get(String.class);
            Assertions.assertEquals("Hello from Guice", result);
        }
    }

    public interface GreetingService {
        String greet();
    }

    public static class GreetingServiceImpl implements GreetingService {
        @Override
        public String greet() {
            return "Hello from Guice";
        }
    }

    @Path("greeting")
    public static class GreetingResource {
        private final GreetingService service;

        @Inject
        public GreetingResource(final GreetingService service) {
            this.service = service;
        }

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String greeting() {
            return service.greet();
        }
    }

    public static class GreetingModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(GreetingService.class).to(GreetingServiceImpl.class);
            bind(GreetingResource.class);
        }
    }
}
