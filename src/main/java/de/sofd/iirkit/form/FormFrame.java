package de.sofd.iirkit.form;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.trolltech.qt.QPair;
import com.trolltech.qt.core.QEventLoop;
import com.trolltech.qt.core.QObject;
import com.trolltech.qt.core.QUrl;
import com.trolltech.qt.gui.QAction;
import com.trolltech.qt.gui.QCloseEvent;
import com.trolltech.qt.gui.QHideEvent;
import com.trolltech.qt.gui.QMainWindow;
import com.trolltech.qt.gui.QShowEvent;
import com.trolltech.qt.gui.QToolBar;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.network.QNetworkRequest;
import com.trolltech.qt.webkit.QWebFrame;
import com.trolltech.qt.webkit.QWebPage;
import com.trolltech.qt.webkit.QWebView;
import de.sofd.util.IdentityHashSet;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Logger;

/**
 * Qt form frame.
 *
 * Not thread-safe; must be run in the Qt thread exclusively.
 *
 * Normally only {@link FormRunner} uses this directly; use FormRunner
 * for displaying forms from a Swing context.
 *
 * @author Olaf Klischat
 */
 public class FormFrame extends QMainWindow {
    static final Logger logger = Logger.getLogger(FormFrame.class);

    private QWebView webView;
    private QAction forward;
    private QAction backward;
    private QAction reload;
    private QAction stop;
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    private boolean loadPending = false;
    private final QEventLoop waitForLoadEventLoop = new QEventLoop();
    private Exception loadError = null;

    private final Collection<FormListener> formListeners = new IdentityHashSet<FormListener>();

    public FormFrame() {
        this(null);
    }

    public FormFrame(QWidget parent) {
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
                fireFormEvent(new FormEvent(FormEvent.Type.FORM_SUBMITTED, request.url().toString(), requestParams));
                return false;
            } else {
                return super.acceptNavigationRequest(frame, request, type);
            }
        }

        @Override
        protected void javaScriptConsoleMessage(String message, int lineNumber, String sourceID) {
            super.javaScriptConsoleMessage(message, lineNumber, sourceID);
            logger.info("JS message: " + sourceID + ":" + lineNumber + ": " + message);
        }

    }

    private void loadStarted() {
    }

    private void loadProgress(int x) {
        statusBar().showMessage("Loading: " + x + " %");
    }

    private void loadDone() {
        try {
            logger.debug("loading JS utilities...");
            runJavascriptStreamInForm(this.getClass().getResourceAsStream("jquery-1.7.2.min.js"));
            runJavascriptStreamInForm(this.getClass().getResourceAsStream("URI.min.js"));
            runJavascriptStreamInForm(this.getClass().getResourceAsStream("formutils.js"));
            logger.debug("DONE loading JS utilities.");
            statusBar().showMessage("Loaded.");
            loadError = null;
        } catch (Exception e) {
            statusBar().showMessage("ERROR: " + e.getLocalizedMessage());
            loadError = e;
            throw new IllegalStateException("form loading/JS error: " + e.getLocalizedMessage(), e);
        } finally {
            if (loadPending) {
                waitForLoadEventLoop.exit();
            }
        }
    }

    public void setUrl(String url) {
        webView.load(new QUrl(url));
        loadPending = true;
        try {
            waitForLoadEventLoop.exec();
        } finally {
            loadPending = false;
            logger.debug("waitForLoadEventLoop finished.");
        }
        if (loadError != null) {
            throw new IllegalStateException("form load error: " + loadError, loadError);
        }
        fireFormEvent(new FormEvent(FormEvent.Type.FORM_OPENED));
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

    @Override
    protected void showEvent(QShowEvent event) {
        fireFormEvent(new FormEvent(FormEvent.Type.FORM_SHOWN));
    }

    @Override
    protected void hideEvent(QHideEvent event) {
        fireFormEvent(new FormEvent(FormEvent.Type.FORM_HIDDEN));
    }

    @Override
    protected void closeEvent(QCloseEvent event) {
        webView.loadProgress.disconnect(this);
        webView.loadFinished.disconnect(this);
        fireFormEvent(new FormEvent(FormEvent.Type.FORM_CLOSED));
    }

    public void addFormListener(FormListener l) {
        formListeners.add(l);
    }

    public void removeFormListener(FormListener l) {
        formListeners.remove(l);
    }

    protected void fireFormEvent(FormEvent evt) {
        for (FormListener l : formListeners) {
            switch (evt.getType()) {

            case FORM_OPENED:
                l.formOpened(evt);
                break;

            case FORM_CLOSED:
                l.formClosed(evt);
                break;

            case FORM_SHOWN:
                l.formShown(evt);
                break;

            case FORM_HIDDEN:
                l.formHidden(evt);
                break;

            case FORM_SUBMITTED:
                l.formSubmitted(evt);
                break;
            }
        }
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

}
