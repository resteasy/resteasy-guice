package dev.resteasy.guice;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.util.GetRestful;

import com.google.inject.Binding;
import com.google.inject.Injector;

import dev.resteasy.guice._private.LogMessages;
import dev.resteasy.guice._private.Messages;

public class ModuleProcessor {

    private final Registry registry;
    private final ResteasyProviderFactory providerFactory;

    public ModuleProcessor(final Registry registry, final ResteasyProviderFactory providerFactory) {
        this.registry = registry;
        this.providerFactory = providerFactory;
    }

    public void processInjector(final Injector injector) {
        List<Binding<?>> rootResourceBindings = new ArrayList<Binding<?>>();
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
        for (Binding<?> binding : rootResourceBindings) {
            Class<?> beanClass = (Class<?>) binding.getKey().getTypeLiteral().getType();
            final ResourceFactory resourceFactory = new GuiceResourceFactory(binding.getProvider(), beanClass);
            LogMessages.LOGGER.info(Messages.MESSAGES.registeringFactory(beanClass.getName()));
            registry.addResourceFactory(resourceFactory);
        }
    }
}
