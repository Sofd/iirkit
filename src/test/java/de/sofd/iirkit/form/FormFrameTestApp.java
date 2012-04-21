package de.sofd.iirkit.form;

import com.trolltech.qt.gui.QApplication;

/**
 *
 * @author olaf
 */
public class FormFrameTestApp {

    public FormFrameTestApp(String[] args) {
        FormFrame formFrame = new FormFrame("file:///home/olaf/hieronymusr/iirkit-test/ecrf/312046_11.html", null);
        formFrame.show();
    }

    public static void main(final String[] args) throws Exception {
        QApplication.initialize(new String[0]);
        new FormFrameTestApp(args);
        QApplication.exec();
        System.err.println("QT thread finished.");
   }

}
