package de.sofd.iirkit.form;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private String initialFormContent;
    private QAction forward;
    private QAction backward;
    private QAction reload;
    private QAction stop;
    private FormDoneEvent formDoneEvent;
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    public FormFrame(String url, String initialFormContent) {
        this(null, url, initialFormContent);
    }

    public FormFrame(QWidget parent, final String url, String initialFormContent) {
        super(parent);
        this.initialFormContent = initialFormContent;

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
        try {
            runJavascriptStreamInForm(this.getClass().getResourceAsStream("jquery-1.7.2.min.js"));
            runJavascriptStreamInForm(this.getClass().getResourceAsStream("URI.min.js"));
            runJavascriptStreamInForm(this.getClass().getResourceAsStream("formutils.js"));
            if (null != initialFormContent) {
                setFormContents(initialFormContent);
            }
            statusBar().showMessage("Loaded.");
        } catch (Exception e) {
            statusBar().showMessage("ERROR: " + e.getLocalizedMessage());
            //TODO: set some permant error state
            throw new IllegalStateException("form loading/JS error: " + e.getLocalizedMessage(), e);
        }
    }

    private void debug1() {
        logger.info("debug1");
        try {
            String paramString = "COMP01_Seq4_COMPISC5=1&foo=bar&COMP01_Seq2_COMPISC5=1&sex=male&weight=45&TRIG01_Seq1_TRIGPERF=1&IMAGE01_Seq1_IMAGAV=&examinationTime=1.2.34&ailments=cancer&ailments=syphillis&headeareyenosethroat=normal&height=123&respiratory=normal&ethnicGroup=black&COMP01_Seq1_COMPISC5=2&age=910&ok=OK&COMP01_Seq3_COMPISC5=2&IMAGE01_Seq1_FILMNO=&cardiovascular=abnormal&gastrointestinal=abnormal&birthData=5.6.78";
            setFormContents(paramString);
        } catch (Exception e) {
            logger.error("error running javascript code: " + e.getLocalizedMessage(), e);
        }
    }

    public void setFormContents(Multimap<String, String> params) {
        setFormContents(FormUtils.paramsToQueryString(params));
        //TODO: figure out how to convert params into a JS object (map) directly
        //  rather than converting it to a string first
    }

    public void setFormContents(String formContentsAsQueryString) {
        QWebFrame frame = getWebFrame();
        QObject sw = new QObject();
        sw.setObjectName(formContentsAsQueryString); //hack -- misuse objectName for the paramString
        frame.addToJavaScriptWindowObject("__paramString", sw);
        frame.evaluateJavaScript("__fillForm(window.__paramString.objectName)");
        //TODO: log JS errors
    }

    /**
     * Must be called in QT thread.
     *
     * @param jsCode
     */
    public void runJavascriptInForm(String jsCode) {
        //TODO: synchronize with loadDone() etc.?
        try {
            getWebFrame().evaluateJavaScript(jsCode);
        } catch (Exception e) {
            logger.error("error running javascript code: " + e.getLocalizedMessage(), e);
        }
        //TODO: log JS errors
    }

    protected QWebFrame getWebFrame() {
        if (webView == null) {
            throw new IllegalStateException("web view not present");
        }
        QWebPage page = webView.page();
        if (page == null) {
            throw new IllegalStateException("web page not present");
        }
        QWebFrame frame = page.currentFrame();
        if (frame == null) {
            throw new IllegalStateException("web frame not present");
        }
        return frame;
    }

    public void runJavascriptStreamInForm(InputStream is) throws IOException {
        runJavascriptInForm(CharStreams.toString(new InputStreamReader(is, "utf-8")));
    }

    private Runnable formDoneCallback;

    @Override
    protected void closeEvent(QCloseEvent event) {
        webView.loadProgress.disconnect(this);
        webView.loadFinished.disconnect(this);
        formDoneEvent = new FormDoneEvent();  //"cancelled" event
        if (null != formDoneCallback) {
            logger.debug("FormFrame#closeEvent formDoneCallback.run()");
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

        FormFrame widget = new FormFrame("/home/olaf/hieronymusr/iirkit-test/ecrf/312046_11.html", null);
        widget.show();

        QApplication.exec();
    }

}
