package de.sofd.iirkit;

import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author sofd
 */
public class AwtExceptionHandler {

    static final Logger logger = Logger.getLogger(AwtExceptionHandler.class);

    public static String message;

    public void handle(Throwable throwable) {
        try {
            if (throwable != null) {
                logger.error("handle - throwable != null", throwable);
                ////hacks to try to extract the root exception message for displaying better error messages
                String displayErrorMsg = throwable.getLocalizedMessage();
                if (throwable instanceof Error) {
                    //for uncaught exception thrown during startup: hack to get at the "cause" exception's message,
                    //which jdesktop fails to include in the toplevel message (cf. org.jdesktop.application.Application#doCreateAndShowGUI)
                    if (throwable.getMessage().startsWith("Application ") &&
                        throwable.getMessage().endsWith("failed to launch")) { //heuristics to detect startup exceptions
                        Throwable cause = throwable.getCause();
                        if (cause != null) {
                            displayErrorMsg += ("\n" + cause.getLocalizedMessage());
                        }
                    } else if (throwable.getCause() instanceof InvocationTargetException) { //heuristics to detect exceptions thrown in a jdesktop action handler
                        Throwable cause = throwable.getCause();
                        //unwind chains of InvocationTargetExceptions (which again don't preserve the root exception message)
                        while (cause instanceof InvocationTargetException) {
                            cause = ((InvocationTargetException)cause).getTargetException();
                        }
                        displayErrorMsg += ("\n" + cause.getLocalizedMessage());
                    }
                }
                JOptionPane.showMessageDialog(new JFrame(), displayErrorMsg +  " " + StringUtils.trimToEmpty(message), "Error", JOptionPane.ERROR_MESSAGE);
                Toolkit.getDefaultToolkit().beep();
                System.exit(1);
            }
        } catch (Exception ex) {
            logger.error("handle - throwable == null", ex);
            Toolkit.getDefaultToolkit().beep();
            System.exit(1);
        }
    }

}
