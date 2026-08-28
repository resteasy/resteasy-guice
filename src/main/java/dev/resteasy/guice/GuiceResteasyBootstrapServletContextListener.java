/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import org.jboss.resteasy.plugins.server.servlet.ResteasyBootstrap;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import dev.resteasy.guice._private.LogMessages;
import dev.resteasy.guice._private.Messages;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

/**
 * A {@link ServletContextListener} that bootstraps RESTEasy with Guice. Register it in {@code web.xml} (or an
 * equivalent programmatic servlet setup) alongside the RESTEasy dispatcher servlet.
 * <p>
 * On {@link #contextInitialized(ServletContextEvent) context initialization} it builds a Guice {@link Injector}
 * from the configured {@link Module}s and hands it to a {@link ModuleProcessor}, which registers every bound
 * {@code @Path} root resource and {@code @Provider} type with RESTEasy. Modules are, by default, taken from the
 * comma-separated {@code resteasy.guice.modules} context-param and instantiated with their no-arg constructor;
 * the Guice {@link Stage} may be set with the {@code resteasy.guice.stage} context-param.
 * <p>
 * The behavior can be customized by subclassing and overriding {@link #getModules(ServletContext)},
 * {@link #getStage(ServletContext)}, or {@link #withInjector(Injector)}; register the subclass as the listener.
 * If a parent {@link Injector} is available via {@link Inject field injection}, a child injector is created from
 * it instead. No-arg {@link PostConstruct} and {@link PreDestroy} methods on module instances are invoked on
 * context initialization and destruction, respectively.
 */
public class GuiceResteasyBootstrapServletContextListener extends ResteasyBootstrap implements ServletContextListener {

    private List<? extends Module> modules;
    @Inject
    private Injector parentInjector = null;

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        super.contextInitialized(event);
        final ServletContext context = event.getServletContext();
        final ResteasyDeployment deployment = (ResteasyDeployment) context.getAttribute(ResteasyDeployment.class.getName());
        final Registry registry = deployment.getRegistry();
        final ResteasyProviderFactory providerFactory = deployment.getProviderFactory();
        final ModuleProcessor processor = new ModuleProcessor(registry, providerFactory);
        final List<? extends Module> modules = getModules(context);
        final Stage stage = getStage(context);
        Injector injector;

        if (parentInjector != null) {
            injector = parentInjector.createChildInjector(modules);
        } else {
            if (stage == null) {
                injector = Guice.createInjector(modules);
            } else {
                injector = Guice.createInjector(stage, modules);
            }
        }
        withInjector(injector);
        processor.processInjector(injector);

        //load parent injectors
        while (injector.getParent() != null) {
            injector = injector.getParent();
            processor.processInjector(injector);
        }
        this.modules = modules;
        triggerAnnotatedMethods(PostConstruct.class);
    }

    /**
     * Override this method to interact with the {@link Injector} after it has been created. The default is a no-op.
     *
     * @param injector the fully-created injector for this deployment
     */
    protected void withInjector(Injector injector) {
    }

    /**
     * Override this method to set the Guice {@link Stage}. By default, it is taken from the
     * {@code resteasy.guice.stage} context-param, or {@code null} (Guice's own default) if that is not set.
     *
     * @param context the servlet context for this deployment
     *
     * @return the Guice {@link Stage} to create the injector with, or {@code null} to use Guice's default
     */
    protected Stage getStage(ServletContext context) {
        final String stageAsString = context.getInitParameter("resteasy.guice.stage");
        if (stageAsString == null) {
            return null;
        }
        try {
            return Stage.valueOf(stageAsString.trim());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(Messages.MESSAGES.injectorStageNotProperlyDefined(stageAsString));
        }
    }

    /**
     * Override this method to instantiate your {@link Module}s yourself, for example when a module needs
     * constructor arguments. The default reads the comma-separated {@code resteasy.guice.modules} context-param
     * and instantiates each listed class with its no-arg constructor.
     *
     * @param context the servlet context for this deployment
     *
     * @return the modules to build the injector from; never {@code null}
     */
    protected List<? extends Module> getModules(final ServletContext context) {
        final List<Module> result = new ArrayList<>();
        final String modulesString = context.getInitParameter("resteasy.guice.modules");
        if (modulesString != null) {
            final String[] moduleStrings = modulesString.trim().split(",");
            for (final String moduleString : moduleStrings) {
                try {
                    LogMessages.LOGGER.info(Messages.MESSAGES.foundModule(moduleString));
                    final Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(moduleString.trim());
                    final Module module = (Module) clazz.getDeclaredConstructor().newInstance();
                    result.add(module);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        triggerAnnotatedMethods(PreDestroy.class);
    }

    private void triggerAnnotatedMethods(final Class<? extends Annotation> annotationClass) {
        for (final Module module : this.modules) {
            final Method[] methods = module.getClass().getMethods();
            for (final Method method : methods) {
                if (method.isAnnotationPresent(annotationClass)) {
                    if (method.getParameterTypes().length > 0) {
                        LogMessages.LOGGER.warn(Messages.MESSAGES.cannotExecute(module.getClass().getSimpleName(),
                                annotationClass.getSimpleName(), method.getName()));
                        continue;
                    }
                    try {
                        method.invoke(module);
                    } catch (InvocationTargetException | IllegalAccessException ex) {
                        LogMessages.LOGGER
                                .warn(Messages.MESSAGES.problemRunningAnnotationMethod(annotationClass.getSimpleName()), ex);
                    }
                }
            }
        }
    }
}
