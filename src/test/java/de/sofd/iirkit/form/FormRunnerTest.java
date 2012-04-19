package de.sofd.iirkit.form;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 *
 * @author olaf
 */
public class FormRunnerTest {

    private static final Logger logger = Logger.getLogger(FormRunnerTest.class);

    private FormRunner fr;

    public FormRunnerTest() {
        fr = new FormRunner(null);
        JFrame f = new JFrame("FormRunner interactive test");
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        f.getContentPane().add(toolbar, BorderLayout.PAGE_START);
        toolbar.add(new AbstractAction("start") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fr.start("file:///home/olaf/hieronymusr/iirkit-test/ecrf/312046_11.html");
            }
        });
        toolbar.add(new AbstractAction("cancel") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fr.cancel();
            }
        });
        f.pack();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
        fr.addFormDoneListener(new FormDoneListener() {

            @Override
            public void formSubmitted(FormDoneEvent event) {
                logger.info("formSubmitted event: " + event);
            }

            @Override
            public void formCancelled(FormDoneEvent event) {
                logger.info("formCancelled event: " + event);
            }
        });
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FormRunnerTest();
            }
        });
    }

}
