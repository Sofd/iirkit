package de.sofd.iirkit.form;

import com.trolltech.qt.core.QUrl;
import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QCloseEvent;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QToolBar;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.webkit.QWebView;

/**
 *
 * @author olaf
 */
public class FormFrame extends QMainWindow {
    private QWebView browser;

    private QAction forward;
    private QAction backward;
    private QAction reload;
    private QAction stop;

    public FormFrame() {
        this(null);
    }

    public FormFrame(QWidget parent) {
        super(parent);

        browser = new QWebView();

        QToolBar toolbar = addToolBar("Actions");
        backward = toolbar.addAction("Backward");
        forward = toolbar.addAction("Forward");
        reload = toolbar.addAction("Reload");
        stop = toolbar.addAction("Stop");
        toolbar.setFloatable(false);
        toolbar.setMovable(false);

        setCentralWidget(browser);
        statusBar().show();

        setWindowTitle("eCRF");
        //setWindowIcon(new QIcon("classpath:com/trolltech/images/qt-logo.png"));

        browser.loadProgress.connect(this, "loadProgress(int)");
        browser.loadFinished.connect(this, "loadDone()");
        browser.urlChanged.connect(this, "urlChanged(QUrl)");

        forward.triggered.connect(browser, "forward()");
        backward.triggered.connect(browser, "back()");
        reload.triggered.connect(browser, "reload()");
        stop.triggered.connect(browser, "stop()");

        QApplication.invokeLater(new Runnable() {
            public void run() {
                browser.load(new QUrl("http://www.google.de"));
            }
        });
    }

    public void urlChanged(QUrl url) {
        System.out.println("URL changed to: " + url.toString());
    }

    public void loadDone() {
        statusBar().showMessage("Loaded.");
    }

    public void loadProgress(int x) {
        statusBar().showMessage("Loading: " + x + " %");
    }

    private Runnable windowCloseCallback;

    @Override
    protected void closeEvent(QCloseEvent event) {
        browser.loadProgress.disconnect(this);
        browser.loadFinished.disconnect(this);
        if (null != windowCloseCallback) {
            windowCloseCallback.run();
        }
    }

    public void setMainWindowCloseCallback(Runnable r) {
        windowCloseCallback = r;
    }

    public static void main(String args[]) {
        QApplication.initialize(args);

        FormFrame widget = new FormFrame();
        widget.show();

        QApplication.exec();
    }

}
