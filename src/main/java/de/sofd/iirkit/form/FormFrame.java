package de.sofd.iirkit.form;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.StatusTextEvent;
import org.eclipse.swt.browser.StatusTextListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;

import de.sofd.util.IdentityHashSet;

/**
 * SWT form frame.
 *
 * Not thread-safe; must be run in the SWT thread exclusively.
 *
 * Normally only {@link FormRunner} uses this directly; use FormRunner
 * for displaying forms from a Swing context.
 *
 * @author Olaf Klischat
 */
 public class FormFrame {
    static final Logger logger = Logger.getLogger(FormFrame.class);

    private final Display display;
    private final Shell formShell;
    private Browser browser;
    private Label statusLine;
    private final Map<String, Object> attributes = new HashMap<String, Object>();

    private boolean loadPending = false;
    private Exception loadError = null;
    private boolean formLoaded = false;
    private Boolean pendingIsFormEnabled = null;

    private final Collection<FormListener> formListeners = new IdentityHashSet<FormListener>();
    
    private boolean nestedEventLoopExitRequested = false;

    public FormFrame(Display display) {
        this(display, null);
    }

    public FormFrame(Shell parentShell) {
        this(null, parentShell);
    }

    protected FormFrame(Display display, Shell parentShell) {
        if (display != null) {
            this.formShell = new Shell(display);
            this.display = display;
        } else {
            this.formShell = new Shell(parentShell);
            this.display = parentShell.getDisplay();
        }
        formShell.setText("eCRF");
        formShell.setSize(900, 600);

        GridLayout gl = new GridLayout();
        gl.numColumns = 1;
        formShell.setLayout(gl);

        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;

        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        //browser = new Browser(formShell, SWT.WEBKIT | SWT.BORDER);
        browser = new Browser(formShell, SWT.BORDER);
        //browser = new Browser(formShell, SWT.MOZILLA | SWT.BORDER);
        browser.setLayoutData(gd);
        
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        statusLine = new Label(formShell, SWT.LEFT | SWT.SHADOW_ETCHED_IN);
        statusLine.setLayoutData(gd);

        browser.addLocationListener(new LocationListener() {
            
            private boolean pathIsEcrfSubmit(String path) {
                String lastElt = path;
                int lastSlash = path.lastIndexOf("/");
                if (-1 != lastSlash) {
                    lastElt = path.substring(1 + lastSlash);
                }
                return "submit_ecrf".equals(lastElt);
            }
            
            @Override
            public void changing(LocationEvent event) {
                URL url;
                try {
                    url = new URL(event.location);
                    if (pathIsEcrfSubmit(url.getPath())) {
                        logger.debug("form submitted: " + url);
                        Multimap<String,String> requestParams = ArrayListMultimap.create();
                        if (url.getQuery() != null) {
                            for (String nameEqValue : url.getQuery().split("&")) {
                                String[] nameVal = nameEqValue.split("=");
                                if (nameVal.length == 1 && nameEqValue.endsWith("=")) {
                                    nameVal = new String[] {nameVal[0], ""};
                                }
                                if (nameVal.length != 2) {
                                    logger.warn("query parameter doesn't contain exactly one equals sign; ignored: " + nameEqValue);
                                } else {
                                    try {
                                        requestParams.put(nameVal[0], URLDecoder.decode(nameVal[1], "utf-8"));
                                    } catch (UnsupportedEncodingException e) {
                                        throw new IllegalStateException("query parameter not decodable: " + nameEqValue, e);
                                    }
                                }
                            }
                        }
                        if (requestParams.isEmpty()) {
                            throw new IllegalStateException("form submit with no (GET) parameters. Did you accidentally set the form's method to POST?");
                        }
                        event.doit = false;
                        fireFormEvent(new FormEvent(FormEvent.Type.FORM_SUBMITTED, url.toString(), requestParams));
                    }
                } catch (MalformedURLException e) {
                    statusLine.setText("Malformed URL: " + event.location);
                }
            }
            
            @Override
            public void changed(LocationEvent event) {
            }
        });
        
        browser.addProgressListener(new ProgressAdapter() {
            @Override
            public void completed(ProgressEvent event) {
                try {
                    logger.debug("loading JS utilities...");
                    runJavascriptStreamInForm(this.getClass().getResourceAsStream("jquery-1.7.2.min.js"));
                    runJavascriptInForm("$__iirkit_jquery = jQuery.noConflict(); null");
                    runJavascriptStreamInForm(this.getClass().getResourceAsStream("URI.min.js"));
                    //TODO: URI.js noConflict? Or try to get rid of it.
                    runJavascriptStreamInForm(this.getClass().getResourceAsStream("formutils.js"));
                    logger.debug("DONE loading JS utilities.");
                    //statusLine.setText("Loaded.");
                    formLoaded = true;
                    if (null != pendingIsFormEnabled) {
                        boolean newIsFormEnabled = pendingIsFormEnabled;
                        //we probably would only need to set this if it's false, except
                        // if the user for some reason called setFormEnabled(true) explicitly
                        // and wants this to hit the JS code.
                        pendingIsFormEnabled = null;
                        setFormEnabled(newIsFormEnabled);
                    }
                    loadError = null;
                } catch (Exception e) {
                    statusLine.setText("ERROR: " + e.getLocalizedMessage());
                    loadError = e;
                    throw new IllegalStateException("form loading/JS error: " + e.getLocalizedMessage(), e);
                } finally {
                    if (loadPending) {
                        exitNestedEventLoop();
                    }
                }
            }
        });
        
        //TODO: exitNestedEventLoop() on page load errors too.
        // See also http://stackoverflow.com/questions/14244301/swt-browser-how-to-detect-page-load-errors
        
        browser.addStatusTextListener(new StatusTextListener() {
            @Override
            public void changed(StatusTextEvent event) {
                statusLine.setText(event.text);
            }
        });

        formShell.addListener(SWT.Close, new Listener() {
            public void handleEvent(Event event) {
                event.doit = false;
                hide();
            }
        });

        formShell.open();
    }

    public boolean isFormLoaded() {
        return formLoaded;
    }
    
    public void setUrl(String url) {
        formLoaded = false;
        pendingIsFormEnabled = null;
        browser.setUrl(url);
        loadPending = true;
        try {
            runNestedEventLoop();
        } finally {
            loadPending = false;
            logger.debug("waitForLoadEventLoop finished.");
        }
        if (loadError != null) {
            throw new IllegalStateException("form load error: " + loadError, loadError);
        }
        fireFormEvent(new FormEvent(FormEvent.Type.FORM_OPENED));
    }
    
    private void runNestedEventLoop() {
        nestedEventLoopExitRequested = false;
        while (!nestedEventLoopExitRequested) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }
    
    private void exitNestedEventLoop() {
        nestedEventLoopExitRequested = true;
    }

    public void setFormContents(Multimap<String, String> params) {
        setFormContents(FormUtils.paramsToQueryString(params));
    }

    /**
     * 
     * @param formContentsAsQueryString properly encoded URL query string
     */
    public void setFormContents(String formContentsAsQueryString) {
        browser.execute("__fillForm(\"" + formContentsAsQueryString + "\")");
    }

    /**
     * Must be called in SWT thread.
     *
     * @param jsCode
     */
    public void runJavascriptInForm(String jsCode) {
        //TODO: synchronize with form load etc.?
        try {
            browser.execute(jsCode);
        } catch (Exception e) {
            logger.error("error running javascript code: " + e.getLocalizedMessage(), e);
        }
        //TODO: log JS errors
    }

    /**
     * Enable/disable the form's input controls.
     * 
     * Will be reset after setURL().
     * 
     * @param enabled
     */
    public void setFormEnabled(boolean enabled) {
        if (isFormLoaded()) {
            runJavascriptInForm("__enableForm(" + enabled + ")");
        } else {
            pendingIsFormEnabled = enabled;
        }
    }
    
    public void runJavascriptStreamInForm(InputStream is) throws IOException {
        runJavascriptInForm(CharStreams.toString(new InputStreamReader(is, "utf-8")));
    }

    public void show() {
        formShell.setVisible(true);
        fireFormEvent(new FormEvent(FormEvent.Type.FORM_SHOWN));
    }
    
    public void hide() {
        formShell.setVisible(false);
        fireFormEvent(new FormEvent(FormEvent.Type.FORM_HIDDEN));
    }
    
    public void close() {
        formShell.close();
        fireFormEvent(new FormEvent(FormEvent.Type.FORM_DELETED));
    }
    
    public void setGeometry(int x, int y, int width, int height) {
        formShell.setBounds(x, y, width, height);
    }

    public void addFormListener(FormListener l) {
        formListeners.add(l);
    }

    public void removeFormListener(FormListener l) {
        formListeners.remove(l);
    }

    protected void fireFormEvent(FormEvent evt) {
        for (FormListener l : Lists.newArrayList(formListeners)) {
            switch (evt.getType()) {

            case FORM_OPENED:
                l.formOpened(evt);
                break;

            case FORM_DELETED:
                formLoaded = false;
                l.formDeleted(evt);
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
    
    public Shell getShell() {
        return formShell;
    }

}
