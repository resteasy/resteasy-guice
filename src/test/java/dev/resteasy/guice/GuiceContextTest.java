/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.inject.Binder;
import com.google.inject.Module;

public class GuiceContextTest {
    @RegisterExtension
    private static final ResteasyGuiceTestExtension TEST_EXTENSION = new ResteasyGuiceTestExtension(TestModule.class);

    @Test
    public void testMethodInjection() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(TEST_EXTENSION.getBaseUri()).path("method").request().get()) {
                Assertions.assertEquals(200, response.getStatus(), () -> "Expected a 200 status but got %d: %s"
                        .formatted(response.getStatus(), response.readEntity(String.class)));
                Assertions.assertEquals("method", response.readEntity(String.class));
            }
        }
    }

    @Test
    public void testFieldInjection() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(TEST_EXTENSION.getBaseUri()).path("field").request().get()) {
                Assertions.assertEquals(200, response.getStatus(), () -> "Expected a 200 status but got %d: %s"
                        .formatted(response.getStatus(), response.readEntity(String.class)));
                Assertions.assertEquals("field", response.readEntity(String.class));
            }
        }
    }

    @Test
    public void testParamConversion() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(TEST_EXTENSION.getBaseUri()).path("test")
                    .queryParam("values", "1,2,3").request().get()) {
                Assertions.assertEquals(200, response.getStatus(), () -> "Expected a 200 status but got %d: %s"
                        .formatted(response.getStatus(), response.readEntity(String.class)));
                Assertions.assertEquals("6", response.readEntity(String.class));
            }
        }
    }

    @Test
    public void testParamConversionAbsent() {
        try (Client client = ClientBuilder.newClient()) {
            try (Response response = client.target(TEST_EXTENSION.getBaseUri()).path("test").request().get()) {
                Assertions.assertEquals(200, response.getStatus(), () -> "Expected a 200 status but got %d: %s"
                        .formatted(response.getStatus(), response.readEntity(String.class)));
                Assertions.assertEquals("[]", response.readEntity(String.class));
            }
        }
    }

    public static class TestModule implements Module {
        @Override
        public void configure(final Binder binder) {
            binder.bind(MethodTestResource.class);
            binder.bind(FieldTestResource.class);
            binder.bind(ConversionTestResource.class);
            binder.bind(IntarrayConverterProvider.class);
        }
    }

    @Path("method")
    public static class MethodTestResource {
        @GET
        public String getName(final @Context UriInfo uriInfo) {
            Assertions.assertNotNull(uriInfo);
            return "method";
        }
    }

    @Path("field")
    public static class FieldTestResource {
        private @Context UriInfo uriInfo;

        @GET
        public String getName() {
            Assertions.assertNotNull(uriInfo);
            return "field";
        }
    }

    @Path("test")
    public static class ConversionTestResource {
        @QueryParam("values")
        Intarray intarray;

        @GET
        public String getName() {
            return intarray == null ? "[]" : String.valueOf(intarray.sum());
        }
    }

    public static class Intarray {
        private int[] values;

        public Intarray() {
        }

        public Intarray(final int[] values) {
            this.values = values;
        }

        @Override
        public String toString() {
            return values == null ? "[]" : Arrays.asList(values).toString();
        }

        public int[] getValues() {
            return values;
        }

        public void setValues(int[] values) {
            this.values = values;
        }

        public int sum() {
            if (values == null) {
                return 0;
            }
            int sum = 0;
            for (int value : values) {
                sum += value;
            }
            return sum;
        }
    }

    @Provider
    public static class IntarrayConverterProvider implements ParamConverterProvider {
        @Override
        public <T> ParamConverter<T> getConverter(final Class<T> tClass, Type type, Annotation[] annotations) {
            return tClass == Intarray.class ? new ParamConverter<T>() {
                @Override
                // for simplicity, does not take "[" and "]" into account
                public T fromString(String s) {
                    String[] strings = s.split("\\s*,\\s*");
                    int[] values = new int[strings.length];
                    for (int i = 0; i < strings.length; i++) {
                        values[i] = Integer.valueOf(strings[i]);
                    }
                    return tClass.cast(new Intarray(values));
                }

                @Override
                public String toString(T t) {
                    return t.toString();
                }
            } : null;
        }
    }

}
