/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice;

import java.util.concurrent.CompletionStage;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import com.google.inject.Provider;

/**
 * A RESTEasy {@link ResourceFactory} backed by a Guice {@link Provider}. Each request obtains a fresh resource
 * instance from {@link Provider#get()} (so the resource's Guice scope governs its lifecycle) and then runs
 * RESTEasy property injection ({@code @Context} fields and setters) on it.
 */
public class GuiceResourceFactory implements ResourceFactory {

    private final Provider<?> provider;
    private final Class<?> scannableClass;
    private PropertyInjector propertyInjector;

    public GuiceResourceFactory(final Provider<?> provider, final Class<?> scannableClass) {
        this.provider = provider;
        this.scannableClass = scannableClass;
    }

    @Override
    public Class<?> getScannableClass() {
        return scannableClass;
    }

    @Override
    public void registered(final ResteasyProviderFactory factory) {
        propertyInjector = factory.getInjectorFactory().createPropertyInjector(scannableClass, factory);
    }

    @Override
    public Object createResource(final HttpRequest request, final HttpResponse response,
            final ResteasyProviderFactory factory) {
        final Object resource = provider.get();
        final CompletionStage<Void> propertyStage = propertyInjector.inject(request, response, resource, true);
        return propertyStage == null ? resource
                : propertyStage
                        .thenApply(v -> resource);
    }

    @Override
    public void requestFinished(final HttpRequest request, final HttpResponse response, final Object resource) {
    }

    @Override
    public void unregistered() {
    }
}
