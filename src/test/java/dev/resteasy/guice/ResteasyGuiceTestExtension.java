/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice;

import java.net.URI;
import java.util.stream.Stream;

import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import com.google.inject.Module;

/**
 * A JUnit 5 extension that boots an embedded Jetty servlet container wired with the
 * {@link GuiceResteasyBootstrapServletContextListener}, exercising the same bootstrap path a real
 * deployment uses. The supplied {@link Module} types are passed to the listener via the
 * {@code resteasy.guice.modules} context-param.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
public class ResteasyGuiceTestExtension implements BeforeAllCallback {

    private final Class<? extends Module>[] modules;
    private URI baseUri;

    @SafeVarargs
    public ResteasyGuiceTestExtension(final Class<? extends Module>... modules) {
        this.modules = modules;
    }

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        final String moduleList = String.join(",", Stream.of(modules)
                .map(Class::getName).toList());
        final Server server = new Server();
        final ServerConnector connector = new ServerConnector(server);
        connector.setPort(0);
        server.addConnector(connector);

        final ServletContextHandler servletContext = new ServletContextHandler();
        servletContext.setContextPath("/");
        servletContext.setInitParameter("resteasy.guice.modules", moduleList);
        servletContext.addEventListener(new GuiceResteasyBootstrapServletContextListener());
        servletContext.addServlet(new ServletHolder(HttpServlet30Dispatcher.class), "/*");
        server.setHandler(servletContext);

        server.start();
        // Stored as an AutoCloseable so Jupiter stops the server when the class-level store is
        // closed (i.e. after all tests in the class run); no explicit AfterAllCallback needed.
        final AutoCloseable shutdownTask = server::stop;
        context.getStore(ExtensionContext.Namespace.create(context.getRequiredTestClass(), context.getUniqueId()))
                .put(ResteasyGuiceTestExtension.class.getName(), shutdownTask);
        baseUri = URI.create("http://localhost:" + connector.getLocalPort());
    }

    public URI getBaseUri() {
        return baseUri;
    }
}
