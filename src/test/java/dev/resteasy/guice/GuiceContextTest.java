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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer;
import org.jboss.resteasy.spi.Dispatcher;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Module;

public class GuiceContextTest {
    private NettyJaxrsServer server;
    private Dispatcher dispatcher;

    @BeforeEach
    public void before() throws Exception {
        server = new NettyJaxrsServer();
        server.setPort(TestPortProvider.getPort());
        server.setRootResourcePath("/");
        server.getDeployment().start();
        server.start();

        dispatcher = server.getDeployment().getDispatcher();
    }

    @AfterEach
    public void after() throws Exception {
        server.stop();
        server = null;
        dispatcher = null;
    }

    @Test
    public void testMethodInjection() {
        final Module module = new Module() {
            @Override
            public void configure(final Binder binder) {
                binder.bind(MethodTestResource.class);
            }
        };
        final ModuleProcessor processor = new ModuleProcessor(dispatcher.getRegistry(), dispatcher.getProviderFactory());
        processor.processInjector(Guice.createInjector(module));
        final TestResource resource = TestPortProvider.createProxy(TestResource.class, TestPortProvider.generateBaseUrl());
        Assertions.assertEquals("method", resource.getName());
        dispatcher.getRegistry().removeRegistrations(MethodTestResource.class);
    }

    @Test
    public void testFieldInjection() {
        final Module module = new Module() {
            @Override
            public void configure(final Binder binder) {
                binder.bind(FieldTestResource.class);
            }
        };
        final ModuleProcessor processor = new ModuleProcessor(dispatcher.getRegistry(), dispatcher.getProviderFactory());
        processor.processInjector(Guice.createInjector(module));
        final TestResource resource = TestPortProvider.createProxy(TestResource.class, TestPortProvider.generateBaseUrl());
        Assertions.assertEquals("field", resource.getName());
        dispatcher.getRegistry().removeRegistrations(FieldTestResource.class);
    }

    @Test
    public void testArbitraryInjection() {
        final Module module = new Module() {
            public void configure(final Binder binder) {
                // currently the order is important, this test does not fail, if we bind the classes in revers order
                binder.bind(ConversionTestResource.class);
                binder.bind(IntarrayConverterProvider.class);
            }
        };
        final ModuleProcessor processor = new ModuleProcessor(dispatcher.getRegistry(), dispatcher.getProviderFactory());
        processor.processInjector(Guice.createInjector(module));
        TestPortProvider.createProxy(TestResource.class, TestPortProvider.generateBaseUrl());
    }

    //@Test // not (yet) supprted
    public void testConstructorInjection() {
        final Module module = new Module() {
            @Override
            public void configure(final Binder binder) {
                binder.bind(ConstructorTestResource.class);
            }
        };
        final ModuleProcessor processor = new ModuleProcessor(dispatcher.getRegistry(), dispatcher.getProviderFactory());
        processor.processInjector(Guice.createInjector(module));
        final TestResource resource = TestPortProvider.createProxy(TestResource.class, TestPortProvider.generateBaseUrl());
        Assertions.assertEquals("constructor", resource.getName());
        dispatcher.getRegistry().removeRegistrations(ConstructorTestResource.class);
    }

    @Path("test")
    public interface TestResource {
        @GET
        String getName();
    }

    @Path("test")
    public static class MethodTestResource {
        @GET
        public String getName(final @Context UriInfo uriInfo) {
            Assertions.assertNotNull(uriInfo);
            return "method";
        }
    }

    @Path("test")
    public static class FieldTestResource {
        private @Context UriInfo uriInfo;

        @GET
        public String getName() {
            Assertions.assertNotNull(uriInfo);
            return "field";
        }
    }

    @Path("test")
    public static class ConstructorTestResource {
        private final UriInfo uriInfo;

        public ConstructorTestResource(@Context final UriInfo uriInfo) {
            this.uriInfo = uriInfo;
        }

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
