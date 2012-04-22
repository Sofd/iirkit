package de.sofd.iirkit.form;

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

    FormFrame formFrame;

    public FormFrameTestApp(String[] args) {
        formFrame = new FormFrame("file:///home/olaf/hieronymusr/iirkit-test/ecrf/312046_11.html", null);
        formFrame.setFormDoneCallback(new Runnable() {
            @Override
            public void run() {
                logger.info("FORM DONE CALLBACK. isFormSubmitted: " + formFrame.getFormDoneEvent().isFormSubmitted());
            }
        });
        formFrame.show();

        QMainWindow controllerFrame = new QMainWindow();
        controllerFrame.setWindowTitle("control");
        QToolBar toolbar = controllerFrame.addToolBar("Actions");
        toolbar.setFloatable(false);
        toolbar.addAction("show").triggered.connect(this, "showForm()");
        toolbar.addAction("close").triggered.connect(this, "closeForm()");
        controllerFrame.show();
    }

    private void showForm() {
        formFrame.show();
    }

    private void closeForm() {
        formFrame.close();
    }

    public static void main(final String[] args) throws Exception {
        QApplication.initialize(new String[0]);
        new FormFrameTestApp(args);
        QApplication.exec();
        System.err.println("QT thread finished.");
   }

}
