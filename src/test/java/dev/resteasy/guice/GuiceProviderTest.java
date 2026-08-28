/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.inject.Binder;
import com.google.inject.Module;

public class GuiceProviderTest {
    @RegisterExtension
    private static final ResteasyGuiceTestExtension TEST_EXTENSION = new ResteasyGuiceTestExtension(TestModule.class);

    @Test
    public void testProvider() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(TEST_EXTENSION.getBaseUri()).path("test").request().get()) {
                Assertions.assertEquals(200, response.getStatus(), () -> "Expected a 200 status but got %d: %s"
                        .formatted(response.getStatus(), response.readEntity(String.class)));
                Assertions.assertEquals("exception", response.readEntity(String.class));
            }
        }
    }

    public static class TestModule implements Module {

        @Override
        public void configure(final Binder binder) {
            binder.bind(TestExceptionProvider.class);
            binder.bind(TestResource.class).to(TestResourceException.class);
        }
    }

    @Path("test")
    public interface TestResource {
        @GET
        String getName();
    }

    public static class TestResourceException implements TestResource {
        @Override
        public String getName() {
            throw new TestException();
        }
    }

    public static class TestException extends RuntimeException {
    }

    @Provider
    public static class TestExceptionProvider implements ExceptionMapper<TestException> {
        @Override
        public Response toResponse(final TestException exception) {
            return Response.ok("exception").build();
        }
    }
}
