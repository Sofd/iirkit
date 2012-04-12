package de.sofd.iirkit.form;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.trolltech.qt.QPair;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.QUrl;
import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QApplication;
import com.trolltech.qt.gui.QCloseEvent;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QToolBar;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.network.QNetworkRequest;
import com.trolltech.qt.webkit.QWebFrame;
import com.trolltech.qt.webkit.QWebPage;
import com.trolltech.qt.webkit.QWebView;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 *
 * @author olaf
 */
 public class FormFrame extends QMainWindow {
    static final Logger logger = Logger.getLogger(FormFrame.class);

    private QWebView webView;
    private QAction forward;
    private QAction backward;
    private QAction reload;
    private QAction stop;
    private FormDoneEvent formDoneEvent;
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    public FormFrame(String url) {
        this(null, url);
    }

    public FormFrame(QWidget parent, final String url) {
        super(parent);

        webView = new QWebView();
        webView.setPage(new EcrfSubmitHandlingWebPage());
        //webView.page().setLinkDelegationPolicy(LinkDelegationPolicy.DelegateAllLinks);

        QToolBar toolbar = addToolBar("Actions");
        backward = toolbar.addAction("<");
        forward = toolbar.addAction(">");
        reload = toolbar.addAction("Reload");
        stop = toolbar.addAction("Stop");
        toolbar.setFloatable(false);
        toolbar.setMovable(false);

        setCentralWidget(webView);
        statusBar().show();

        setWindowTitle("eCRF");
        //setWindowIcon(new QIcon("classpath:com/trolltech/images/qt-logo.png"));

        webView.loadStarted.connect(this, "loadStarted()");
        webView.loadProgress.connect(this, "loadProgress(int)");
        webView.loadFinished.connect(this, "loadDone()");
        //webView.linkClicked.connect(this, "linkClicked(QUrl)");
        //webView.page().linkClicked.connect(this, "linkClicked(QUrl)");
        //webView.urlChanged.connect(this, "urlChanged(QUrl)");

        forward.triggered.connect(webView, "forward()");
        backward.triggered.connect(webView, "back()");
        reload.triggered.connect(webView, "reload()");
        stop.triggered.connect(webView, "stop()");

        QAction debug1 = toolbar.addAction("debug1");
        debug1.triggered.connect(this, "debug1()");

        QApplication.invokeLater(new Runnable() {
            @Override
            public void run() {
                webView.load(new QUrl(url));
            }
        });
    }

    private class EcrfSubmitHandlingWebPage extends QWebPage {

        private boolean pathIsEcrfSubmit(String path) {
            String lastElt = path;
            int lastSlash = path.lastIndexOf("/");
            if (-1 != lastSlash) {
                lastElt = path.substring(1 + lastSlash);
            }
            return "submit_ecrf".equals(lastElt);
        }

        @Override
        protected boolean acceptNavigationRequest(QWebFrame frame, QNetworkRequest request, NavigationType type) {
            if (type == NavigationType.NavigationTypeFormSubmitted && pathIsEcrfSubmit(request.url().path())) {
                logger.debug("form submitted: " + request.url());
                Multimap<String,String> requestParams = ArrayListMultimap.create();
                if (request.url().hasQuery()) {
                    for (QPair<String,String> item : request.url().queryItems()) {
                        requestParams.put(item.first, item.second);
                    }
                }
                formDoneEvent = new FormDoneEvent(request.url().toString(), requestParams);  //"formSubmitted" event
                if (null != formDoneCallback) {
                    formDoneCallback.run();
                }

                return false;
            } else {
                return super.acceptNavigationRequest(frame, request, type);
            }
        }

    }

    private void loadStarted() {
    }

    private void loadProgress(int x) {
        statusBar().showMessage("Loading: " + x + " %");
    }

    private void loadDone() {
        statusBar().showMessage("Loaded.");
    }

    private void debug1() {
        logger.info("debug1");
        String paramString = "COMP01_Seq4_COMPISC5=1&COMP01_Seq2_COMPISC5=1&sex=male&weight=45&TRIG01_Seq1_TRIGPERF=1&IMAGE01_Seq1_IMAGAV=&examinationTime=1.2.34&ailments=syphillis&headeareyenosethroat=normal&height=123&respiratory=normal&ethnicGroup=black&COMP01_Seq1_COMPISC5=2&age=910&ok=OK&COMP01_Seq3_COMPISC5=2&IMAGE01_Seq1_FILMNO=&cardiovascular=abnormal&gastrointestinal=abnormal&birthData=5.6.78";
        QWebFrame frame = webView.page().currentFrame();
        try {
            QObject sw = new QObject();
            sw.setObjectName(paramString); //hack -- misuse objectName for the paramString
            frame.addToJavaScriptWindowObject("paramString", sw);
            frame.evaluateJavaScript("fillForm(window.paramString.objectName)");
        } catch (Exception e) {
            logger.error("error running javascript code: " + e.getLocalizedMessage(), e);
        }
        //TODO: log JS errors
    }

    /**
     * Must be called in QT thread.
     *
     * @param jsCode
     */
    public void runJavascriptInForm(String jsCode) {
        //TODO: synchronize with loadDone() etc.?
        if (webView == null) {
            logger.error("can't run JS code: web view not present");
            return;
        }
        QWebPage page = webView.page();
        if (page == null) {
            logger.error("can't run JS code: web page not present");
            return;
        }
        QWebFrame frame = page.currentFrame();
        if (frame == null) {
            logger.error("can't run JS code: web frame not present");
            return;
        }
        try {
            frame.evaluateJavaScript(jsCode);
        } catch (Exception e) {
            logger.error("error running javascript code (" + jsCode + "): " + e.getLocalizedMessage(), e);
        }
    }

    private Runnable formDoneCallback;

    @Override
    protected void closeEvent(QCloseEvent event) {
        webView.loadProgress.disconnect(this);
        webView.loadFinished.disconnect(this);
        formDoneEvent = new FormDoneEvent();  //"cancelled" event
        if (null != formDoneCallback) {
            formDoneCallback.run();
        }
    }

    public void setFormDoneCallback(Runnable r) {
        formDoneCallback = r;
    }

    public FormDoneEvent getFormDoneEvent() {
        return formDoneEvent;
    }

    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public Object putAttribute(String name, Object value) {
        return attributes.put(name, value);
    }

    public Object removeAttribute(String name) {
        return attributes.remove(name);
    }

    public static void main(String args[]) {
        QApplication.initialize(args);

        FormFrame widget = new FormFrame("/home/olaf/hieronymusr/iirkit-test/ecrf/312046_11.html");
        widget.show();

        QApplication.exec();
    }

}
