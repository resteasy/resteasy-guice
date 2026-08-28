/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.GetRestful;

import dev.resteasy.guice._private.LogMessages;
import dev.resteasy.guice._private.Messages;

import com.google.inject.Binding;
import com.google.inject.Injector;

/**
 * Registers the Guice-managed Jakarta REST components of an {@link Injector} with RESTEasy. It walks the
 * injector's explicit bindings and, for each bound type, registers root resources (types recognized by
 * {@link GetRestful#isRootResource(Class)}) with the {@link Registry} and {@code @Provider}-annotated types with
 * the {@link ResteasyProviderFactory}. Because it scans only explicit bindings, every resource and provider must
 * be bound in a Guice {@link com.google.inject.Module}; Guice's just-in-time bindings are not discovered.
 */
public class ModuleProcessor {

    private final Registry registry;
    private final ResteasyProviderFactory providerFactory;

    public ModuleProcessor(final Registry registry, final ResteasyProviderFactory providerFactory) {
        this.registry = registry;
        this.providerFactory = providerFactory;
    }

    /**
     * Registers the root resources and providers bound in the given injector with RESTEasy.
     *
     * @param injector the injector whose bindings should be registered
     */
    public void processInjector(final Injector injector) {
        final List<Binding<?>> rootResourceBindings = new ArrayList<>();
        for (final Binding<?> binding : injector.getBindings().values()) {
            final Class<?> type = binding.getKey().getTypeLiteral().getRawType();
            if (type != null) {
                if (GetRestful.isRootResource(type)) {
                    // deferred registration
                    rootResourceBindings.add(binding);
                }
                if (type.isAnnotationPresent(Provider.class)) {
                    LogMessages.LOGGER.info(Messages.MESSAGES.registeringProviderInstance(type.getName()));
                    providerFactory.registerProviderInstance(binding.getProvider().get());
                }
            }
        }
        for (final Binding<?> binding : rootResourceBindings) {
            final Class<?> beanClass = (Class<?>) binding.getKey().getTypeLiteral().getType();
            final ResourceFactory resourceFactory = new GuiceResourceFactory(binding.getProvider(), beanClass);
            LogMessages.LOGGER.info(Messages.MESSAGES.registeringFactory(beanClass.getName()));
            registry.addResourceFactory(resourceFactory);
        }
    }
}
