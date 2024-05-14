/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.resteasy.guice._private;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.Message.Format;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = "RESTEASY-GUICE")
public interface Messages {
    Messages MESSAGES = org.jboss.logging.Messages.getBundle(MethodHandles.lookup(), Messages.class);
    int BASE = 11000;

    @Message(id = 100, value = "Cannot execute expected module {0}''s @{1} method {2} because it has unexpected parameters: skipping.", format = Format.MESSAGE_FORMAT)
    String cannotExecute(String className, String annotation, String methodName);

    @Message(id = 102, value = "found module: %s")
    String foundModule(String module);

    @Message(id = 103, value = "Injector stage is not defined properly. %s is wrong value. Possible values are PRODUCTION, DEVELOPMENT, TOOL.")
    String injectorStageNotProperlyDefined(String stage);

    @Message(id = 104, value = "Problem running annotation method @%s")
    String problemRunningAnnotationMethod(String annotation);

    @Message(id = 105, value = "registering factory for %s")
    String registeringFactory(String className);

    @Message(id = 106, value = "registering provider instance for %s")
    String registeringProviderInstance(String className);
}
