package de.sofd.iirkit;

import java.awt.Toolkit;
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
                JOptionPane.showMessageDialog(new JFrame(), throwable.getMessage() +  " " + StringUtils.trimToEmpty(message), "Error", JOptionPane.ERROR_MESSAGE);
                logger.error("handle - throwable != null", throwable);
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
