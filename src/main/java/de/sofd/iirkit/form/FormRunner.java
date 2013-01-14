package de.sofd.iirkit.form;

import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import de.sofd.iirkit.App;
import de.sofd.util.IdentityHashSet;

/**
 * FormRunner. Manages a single form window, displaying an HTML (form) page and
 * handling form submits etc.
 * <p>
 * Usage: Create a FormRunner, register a {@link FormListener} to be informed
 * about any state changes, call any of FormRunner#openForm to display the form
 * and load a URL, and optionally fill the form. You may also fill the form
 * later using {@link #setFormContents(com.google.common.collect.Multimap) },
 * or call #openForm again to load a different form. {@link #hideForm()} hides
 * the form without losing its state; it can be re-shown using {@link #showForm()}
 * at any time. When the user closes the form interactively, this has the same
 * effect as calling hide().
 * <p>
 * Runs an SWT event thread internally, and in it a {@link FormFrame}. FormRunner
 * itself is meant to be used from the swing event thread only; it essentially
 * exposes the FormFrame's (SWT-based) functionality to the Swing thread
 * synchronously, and isolates the caller from all the synchronization/MT issues
 * involved.
 *
 * @author Olaf Klischat
 */
public class FormRunner {

    static final Logger logger = Logger.getLogger(FormRunner.class);

    private FormFrame formFrame;
    private final App app;
    private boolean isInSWTExec = false, isInSwingExec = false;
    private final Collection<FormListener> formListeners = new IdentityHashSet<FormListener>();

    private static final CountDownLatch swtInitializedSignal = new CountDownLatch(1);
    
    private static Display display;
    private static boolean formThreadShouldExit = false;

    private static Runnable swtEventLoopRunner = new Runnable() {
        @Override
        public void run() {
            display = new Display();
            swtInitializedSignal.countDown();
            while (!formThreadShouldExit) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
            logger.debug("Form thread finished.");
        }
    };
    
    /**
     * There's only one SWT thread that runs continuously and is shared by all
     * FormRunners (until FormRunner.dispose()). All FormRunners (if there is
     * more than one, which is rarely the case) run their UI in this thread.
     */
    private static Thread swtThread;

    /**
     * If you want to run the SWT event loop in a specific, externally created
     * thread, rather than having FormRunner start its own internal thread for
     * the SWT event loop, call this method before instantiating FormRunner for
     * the first time. It'll synchronously run the SWT event loop in the
     * caller's thread.
     */
    public static void runSwt() {
        swtThread = Thread.currentThread();
        swtEventLoopRunner.run();
    }

    protected void swtExec(Runnable r) {
        if (isInSwingExec) {
            //if we're in a swingExec(),
            // calling display.syncExec would lead to a deadlock
            // (we assume that swtExec is called from the Swing thread)
            //TODO: asyncExec() means that the job may run very late/asynchronous
            logger.debug("must run form job asynchronously b/c we're in a Swing invokeAndWait");
            display.asyncExec(r);
        } else {
            boolean wasInSWTExec = isInSWTExec;
            isInSWTExec = true;
            try {
                display.syncExec(r);
            } finally {
                isInSWTExec = wasInSWTExec;
            }
        }
    }

    protected void swingExec(Runnable r) {
        if (isInSWTExec) {
            //if we're in a swtExec(),
            // calling SwingUtilities.invokeAndWait would lead to a deadlock
            // (we assume that swingExec is called from the SWT thread)
            //TODO: invokeLater() means that the job may run very late/asynchronous
            logger.debug("must run Swing job asynchronously b/c we're in a SWT syncExec");
            SwingUtilities.invokeLater(r);
        } else {
            boolean wasInSwingExec = isInSwingExec;
            isInSwingExec = true;
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (InterruptedException ex) {
                throw new RuntimeException("swing invokeAndWait interrupted.", ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException("swing invokeAndWait exception", ex.getCause());
            } finally {
                isInSwingExec = wasInSwingExec;
            }
        }
    }

    public FormRunner(App app) {
        this.app = app;
        if (swtThread == null) {
            swtThread = new Thread(swtEventLoopRunner, "SWT event loop");
            swtThread.start();
        }
        try {
            swtInitializedSignal.await();
        } catch (InterruptedException ex) {
            throw new IllegalStateException("UI thread interrupted. SHOULDN'T HAPPEN", ex);
        }
    }

    public void openForm(final String url) {
        openForm(url, null, null);
    }

    public void openForm(final String url, Rectangle formBounds) {
        openForm(url, formBounds, null);
    }

    /**
     * Open a new form page, optionally with form contents and boundaries.
     * Opens the form frame if it isn't being shown already.
     *
     * @param url
     * @param formBounds
     * @param formContents
     */
    public void openForm(final String url, final Rectangle formBounds, final String formContentsAsQueryString) {
        ensureFormFrameExists();
        swtExec(new Runnable() {
            @Override
            public void run() {
                if (null != url) {
                    formFrame.setUrl(url);
                    if (null != formContentsAsQueryString) {
                        formFrame.setFormContents(formContentsAsQueryString);
                    }
                }
                formFrame.show();
                if (null != formBounds) {
                    formFrame.setGeometry(formBounds.x, formBounds.y, formBounds.width, formBounds.height);
                }
            }
        });
    }

    protected void ensureFormFrameExists() {
        swtExec(new Runnable() {
            @Override
            public void run() {
                if (null == formFrame) {
                    formFrame = new FormFrame(display) {
                        @Override
                        protected void fireFormEvent(final FormEvent evt) {
                            super.fireFormEvent(evt);
                            swingExec(new Runnable() {
                                @Override
                                public void run() {
                                    FormRunner.this.fireFormEvent(evt);
                                }
                            });
                        }
                    };
                }
            }
        });
    }

    public void setFormContents(final Multimap<String, String> params) {
        ensureFormFrameExists();
        swtExec(new Runnable() {
            @Override
            public void run() {
                formFrame.setFormContents(params);
            }
        });
    }

    public void setFormContents(final String formContentsAsQueryString) {
        ensureFormFrameExists();
        swtExec(new Runnable() {
            @Override
            public void run() {
                formFrame.setFormContents(formContentsAsQueryString);
            }
        });

    }

    public void showForm() {
        ensureFormFrameExists();
        swtExec(new Runnable() {
            @Override
            public void run() {
                formFrame.show();
            }
        });
    }
    
    public void bringFormToFront() {
        ensureFormFrameExists();
        swtExec(new Runnable() {
            @Override
            public void run() {
                formFrame.getShell().forceActive();
            }
        });
    }

    /**
     * Enable/disable the form's input controls.
     * 
     * Will be reset after a new form has been loaded.
     * 
     * @param enabled
     */
    public void setFormEnabled(final boolean enabled) {
        swtExec(new Runnable() {
            @Override
            public void run() {
                formFrame.setFormEnabled(enabled);
            }
        });
    }

    public void hideForm() {
        swtExec(new Runnable() {
            @Override
            public void run() {
                if (null != formFrame) {
                    formFrame.hide();
                }
            }
        });
    }

    public void deleteForm() {
        swtExec(new Runnable() {
            @Override
            public void run() {
                if (null != formFrame) {
                    formFrame.close();
                    //TODO: delete the widget to free up its resources -- how?
                    //  (equivalent to "delete widget;" in C++ (where the d'tor would do the work))
                    //  (destroy() is protected)
                    formFrame = null;
                }
            }
        });
        fireFormEvent(new FormEvent(FormEvent.Type.FORM_DELETED));
    }

    public void setFormBounds(final Rectangle formBounds) {
        ensureFormFrameExists();
        swtExec(new Runnable() {
            @Override
            public void run() {
                if (null != formBounds) {
                    formFrame.setGeometry(formBounds.x, formBounds.y, formBounds.width, formBounds.height);
                }
            }
        });
    }

    public FormFrame getFormFrame() {
        return formFrame;
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

    public void runJavascriptInFormAsync(final String jsCode) {
        //TODO: synchronize with form loading
        ensureFormFrameExists();
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                formFrame.runJavascriptInForm(jsCode);
            }
        });
    }

    /**
     * MUST be called when the FormRunner class is no longer used (generally
     * at the end of the application's lifetime).
     */
    public static void dispose() {
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                formThreadShouldExit = true;
            }
        });
    }

}
