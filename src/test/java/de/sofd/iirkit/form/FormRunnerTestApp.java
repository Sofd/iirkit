package de.sofd.iirkit.form;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 *
 * @author Olaf Klischat
 */
public class FormRunnerTestApp {

    private static final Logger logger = Logger.getLogger(FormRunnerTestApp.class);

    private final String FORM1_URL = "file:///home/olaf/hieronymusr/iirkit-test/ecrf/312046_11.html";
    private final String FORM2_URL = "file:///home/olaf/hieronymusr/iirkit-test/ecrf/form2.html";

    private FormRunner fr;

    public FormRunnerTestApp() {
        fr = new FormRunner(null);
        JFrame f = new JFrame("FormRunner interactive test");
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        f.getContentPane().add(toolbar, BorderLayout.PAGE_START);
        //TODO use jdesktop @Action methods / ActionFactory
        toolbar.add(new AbstractAction("show") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fr.showForm();
            }
        });
        toolbar.add(new AbstractAction("hide") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fr.hideForm();
            }
        });
        toolbar.add(new AbstractAction("form1") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fr.openForm(FORM1_URL);
                logger.debug("form1 loaded");
            }
        });
        toolbar.add(new AbstractAction("form2") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fr.openForm(FORM2_URL);
                logger.debug("form2 loaded");
            }
        });
        toolbar.add(new AbstractAction("fill1") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String paramString = "COMP01_Seq4_COMPISC5=1&foo=bar&COMP01_Seq2_COMPISC5=1&sex=male&weight=45&TRIG01_Seq1_TRIGPERF=1&IMAGE01_Seq1_IMAGAV=&examinationTime=1.2.34&ailments=cancer&ailments=syphillis&headeareyenosethroat=normal&height=123&respiratory=normal&ethnicGroup=black&COMP01_Seq1_COMPISC5=2&age=910&ok=OK&COMP01_Seq3_COMPISC5=2&IMAGE01_Seq1_FILMNO=&cardiovascular=abnormal&gastrointestinal=abnormal&birthData=5.6.78";
                fr.setFormContents(paramString);
            }
        });
        toolbar.add(new AbstractAction("fill2") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Multimap<String, String> params = ArrayListMultimap.create();
                params.put("name", "Hans Meier");
                params.put("street", "Blahweg 5");
                params.put("zip", "10178");
                params.put("city", "Berlin");
                fr.setFormContents(params);
            }
        });
        toolbar.add(new AbstractAction("formfill1") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fr.openForm(FORM1_URL);
                String paramString = "COMP01_Seq4_COMPISC5=1&foo=bar&COMP01_Seq2_COMPISC5=1&sex=male&weight=45&TRIG01_Seq1_TRIGPERF=1&IMAGE01_Seq1_IMAGAV=&examinationTime=1.2.34&ailments=cancer&ailments=syphillis&headeareyenosethroat=normal&height=123&respiratory=normal&ethnicGroup=black&COMP01_Seq1_COMPISC5=2&age=910&ok=OK&COMP01_Seq3_COMPISC5=2&IMAGE01_Seq1_FILMNO=&cardiovascular=abnormal&gastrointestinal=abnormal&birthData=5.6.78";
                fr.setFormContents(paramString);
            }
        });
        toolbar.add(new AbstractAction("formfill2") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fr.openForm(FORM2_URL);
                Multimap<String, String> params = ArrayListMultimap.create();
                params.put("name", "Hans Meier");
                params.put("street", "Blahweg 5");
                params.put("zip", "10178");
                params.put("city", "Berlin");
                fr.setFormContents(params);
            }
        });
        toolbar.add(new AbstractAction("clear") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Multimap<String, String> params = ArrayListMultimap.create();
                fr.setFormContents(params);
            }
        });
        toolbar.add(new AbstractAction("delete") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fr.deleteForm();
            }
        });
        toolbar.add(new AbstractAction("exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                FormRunner.dispose();
                System.exit(0);
            }
        });
        f.pack();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
        fr.addFormListener(new FormListener() {
            @Override
            public void formOpened(FormEvent event) {
                logger.info("Event received: " + event);
            }
            @Override
            public void formDeleted(FormEvent event) {
                logger.info("Event received: " + event);
            }
            @Override
            public void formShown(FormEvent event) {
                logger.info("Event received: " + event);
            }
            @Override
            public void formHidden(FormEvent event) {
                logger.info("Event received: " + event);
            }
            @Override
            public void formSubmitted(FormEvent event) {
                logger.info("Event received: " + event);
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new FormRunnerTestApp();
            }
        });
    }

}
