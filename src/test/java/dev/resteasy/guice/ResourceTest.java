/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class ResourceTest {
    @RegisterExtension
    private static final ResteasyGuiceTestExtension TEST_EXTENSION = new ResteasyGuiceTestExtension(TestModule.class);

    @Test
    public void testResourceRegistered() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(TEST_EXTENSION.getBaseUri()).path("test").request().get()) {
                Assertions.assertEquals(200, response.getStatus(), () -> "Expected a 200 status but got %d: %s"
                        .formatted(response.getStatus(), response.readEntity(String.class)));
                Assertions.assertEquals("name", response.readEntity(String.class));
            }
        }
    }

    @Test
    public void testResourceInjected() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(TEST_EXTENSION.getBaseUri()).path("injected-test").request().get()) {
                Assertions.assertEquals(200, response.getStatus(), () -> "Expected a 200 status but got %d: %s"
                        .formatted(response.getStatus(), response.readEntity(String.class)));
                Assertions.assertEquals("injected-name", response.readEntity(String.class));
            }
        }
    }

    public static class TestModule implements Module {

        @Override
        public void configure(final Binder binder) {
            binder.bind(TestResource.class).to(TestResourceSimple.class);
            binder.bind(String.class).annotatedWith(Names.named("name")).toInstance("injected-name");
            binder.bind(TestResourceInjected.class);
        }
    }

    @Path("test")
    public interface TestResource {
        @GET
        String getName();
    }

    public static class TestResourceSimple implements TestResource {
        @Override
        public String getName() {
            return "name";
        }
    }

    @Path("injected-test")
    public static class TestResourceInjected {
        private final String name;

        @Inject
        public TestResourceInjected(@Named("name") final String name) {
            this.name = name;
        }

        @GET
        public String getName() {
            return name;
        }
    }
}
