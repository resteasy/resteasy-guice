package dev.resteasy.guice._private;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

/**
 *
 * @author <a href="ron.sigal@jboss.com">Ron Sigal</a>
 * @version $Revision: 1.1 $
 *
 *          Copyright Aug 27, 2015
 */
@MessageLogger(projectCode = "RESTEASY-GUICE")
public interface LogMessages extends BasicLogger {
    LogMessages LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), LogMessages.class,
            LogMessages.class.getPackage().getName());
}
