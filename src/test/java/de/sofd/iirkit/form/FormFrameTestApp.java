package de.sofd.iirkit.form;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QToolBar;
import org.apache.log4j.Logger;

/**
 *
 * @author olaf
 */
public class FormFrameTestApp {

    private static final Logger logger = Logger.getLogger(FormFrameTestApp.class);

    private final String FORM1_URL = "file:///home/olaf/hieronymusr/iirkit-test/ecrf/312046_11.html";
    private final String FORM2_URL = "file:///home/olaf/hieronymusr/iirkit-test/ecrf/form2.html";

    private final FormFrame formFrame;

    public FormFrameTestApp(String[] args) {
        formFrame = new FormFrame();
        formFrame.addFormListener(new FormListener() {
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
        formFrame.show();

        QMainWindow controllerFrame = new QMainWindow();
        controllerFrame.setWindowTitle("control");
        QToolBar toolbar = controllerFrame.addToolBar("Actions");
        toolbar.setFloatable(false);
        toolbar.addAction("show").triggered.connect(this, "showForm()");
        toolbar.addAction("hide").triggered.connect(this, "hideForm()");
        toolbar.addAction("form1").triggered.connect(this, "form1()");
        toolbar.addAction("form2").triggered.connect(this, "form2()");
        toolbar.addAction("fill1").triggered.connect(this, "fill1()");
        toolbar.addAction("fill2").triggered.connect(this, "fill2()");
        toolbar.addAction("formfill1").triggered.connect(this, "formfill1()");
        toolbar.addAction("formfill2").triggered.connect(this, "formfill2()");
        toolbar.addAction("bugtest").triggered.connect(this, "bugtest()");
        toolbar.addAction("clear").triggered.connect(this, "clear()");
        toolbar.addAction("exit").triggered.connect(this, "exit()");
        controllerFrame.show();
    }

    private void showForm() {
        formFrame.show();
    }

    private void hideForm() {
        formFrame.hide();
    }

    private void form1() {
        formFrame.setUrl(FORM1_URL);
        logger.debug("form1 loaded");
    }

    private void form2() {
        formFrame.setUrl(FORM2_URL);
        logger.debug("form2 loaded");
    }

    private void fill1() {
        String paramString = "COMP01_Seq4_COMPISC5=1&foo=bar&COMP01_Seq2_COMPISC5=1&sex=male&weight=45&TRIG01_Seq1_TRIGPERF=1&IMAGE01_Seq1_IMAGAV=&examinationTime=1.2.34&ailments=cancer&ailments=syphillis&headeareyenosethroat=normal&height=123&respiratory=normal&ethnicGroup=black&COMP01_Seq1_COMPISC5=2&age=910&ok=OK&COMP01_Seq3_COMPISC5=2&IMAGE01_Seq1_FILMNO=&cardiovascular=abnormal&gastrointestinal=abnormal&birthData=5.6.78";
        formFrame.setFormContents(paramString);
    }

    private void fill2() {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("name", "Hans Meier");
        params.put("street", "Blahweg 5");
        params.put("zip", "10178");
        params.put("city", "Berlin");
        formFrame.setFormContents(params);
    }

    private void formfill1() {
        form1();
        fill1();
    }

    private void formfill2() {
        form2();
        fill2();
    }

    private void clear() {
        Multimap<String, String> params = ArrayListMultimap.create();
        formFrame.setFormContents(params);
    }

    private void bugtest() {
        //s.a. doc/todo.txt
        //
        formFrame.close();
        formFrame.show();
        form1();
        //fill1 (interactively) will err after this (see doc/todo.txt)
        // can also be reproduced interactively: start app, close form, show, form1, fill1
    }

    private void exit() {
        QApplication.exit();
    }

    public static void main(final String[] args) throws Exception {
        QApplication.initialize(new String[0]);
        new FormFrameTestApp(args);
        QApplication.exec();
        System.err.println("QT thread finished.");
   }

}
