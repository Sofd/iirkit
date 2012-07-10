package de.sofd.iirkit.form;

import com.google.common.collect.Multimap;
import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.gui.QApplication;
import de.sofd.iirkit.App;
import de.sofd.util.IdentityHashSet;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 * FormRunner. Manages a single form window, displaying an HTML (form) page and
 * handling form submits etc.
 * <p>
 * Runs a Qt event thread internally, and in it a {@link FormFrame}. FormRunner
 * itself is meant to be used from the swing event thread only; it essentially
 * exposes the FormFrame's (Qt-based) functionality to the Swing thread
 * synchronously, and isolates the caller from all the synchronization/MT issues
 * involved.
 * <p>
 * Usage: Create a FormRunner, register an event listener that is called when
 * the runner is finished, call FormRunner#openForm(url)
 * <p>
 * The runner normally finishes when the user has submitted the form, or has
 * canceled the runner (normally by closing the form window without submitting
 * the form). The runner may also be finished externally by calling #stop().
 * <p>
 * This runner also provides isolation from the QT thread that runs internally
 * for displaying the (QT-based) form UIs. To the outside, FormRunner executes
 * in the Swing thread exclusively (this includes event handlers called by
 * FormRunner). (Exception: {@link #setFormShownCallback(java.lang.Runnable) })
 * FormRunner is NOT thread-safe by itself: It must be called from the Swing
 * thread only.
 *
 * @author Olaf Klischat
 */
public class FormRunner {

    static final Logger logger = Logger.getLogger(FormRunner.class);

    private FormFrame formFrame;
    private final App app;
    private boolean isInQtExec = false;
    private final Collection<FormListener> formListeners = new IdentityHashSet<FormListener>();

    private static final CountDownLatch qtInitializedSignal = new CountDownLatch(1);

    /**
     * There's only one QT thread that runs continuously and is shared
     * by all FormRunners (until FormRunner.dispose()). All FormRunners
     * (if there is more than one) run their QT UI in this thread.
     */
    private static Thread qtThread = new Thread("QT event loop") {

        @Override
        public void run() {
            QApplication.initialize(new String[0]);
            QApplication.setQuitOnLastWindowClosed(false);
            qtInitializedSignal.countDown();
            QApplication.exec();
            System.err.println("QT thread finished.");
        }

    };

    protected void qtExec(Runnable r) {
        boolean wasInQtExec = isInQtExec;
        isInQtExec = true;
        try {
            QApplication.invokeAndWait(r);
        } finally {
            isInQtExec = wasInQtExec;
        }
    }

    protected void swingExec(Runnable r) {
        if (isInQtExec) {
            //if we're in a qtExec(),
            // calling SwingUtilities.invokeAndWait would lead to a deadlock
            // (we assume that swingExec is called from the Qt thread)
            //TODO: invokeLater() means that the job may run very late/asynchronous
            SwingUtilities.invokeLater(r);
        } else {
            try {
                SwingUtilities.invokeAndWait(r);
            } catch (InterruptedException ex) {
                throw new RuntimeException("swing invokeAndWait interrupted.", ex);
            } catch (InvocationTargetException ex) {
                throw new RuntimeException("swing invokeAndWait exception", ex.getCause());
            }
        }
    }

    public FormRunner(App app) {
        this.app = app;
        if (!qtThread.isAlive()) {
            qtThread.start();
            try {
                qtInitializedSignal.await();
            } catch (InterruptedException ex) {
                throw new IllegalStateException("UI thread interrupted. SHOULDN'T HAPPEN", ex);
            }
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
        qtExec(new Runnable() {
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
        qtExec(new Runnable() {
            @Override
            public void run() {
                if (null == formFrame) {
                    formFrame = new FormFrame() {
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
        qtExec(new Runnable() {
            @Override
            public void run() {
                formFrame.setFormContents(params);
            }
        });
    }

    public void setFormContents(final String formContentsAsQueryString) {
        ensureFormFrameExists();
        qtExec(new Runnable() {
            @Override
            public void run() {
                formFrame.setFormContents(formContentsAsQueryString);
            }
        });

    }

    public void showForm() {
        ensureFormFrameExists();
        qtExec(new Runnable() {
            @Override
            public void run() {
                formFrame.show();
            }
        });
    }

    public void hideForm() {
        qtExec(new Runnable() {
            @Override
            public void run() {
                if (null != formFrame) {
                    formFrame.hide();
                }
            }
        });
    }

    public void closeForm() {
        qtExec(new Runnable() {
            @Override
            public void run() {
                if (null != formFrame) {
                    formFrame.close();
                    formFrame = null;
                }
            }
        });
    }

    public void setFormBounds(final Rectangle formBounds) {
        ensureFormFrameExists();
        qtExec(new Runnable() {
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

    public void runJavascriptInFormAsync(final String jsCode) {
        //TODO: synchronize with form loading
        ensureFormFrameExists();
        QApplication.invokeLater(new Runnable() {
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
        QApplication.invokeLater(new Runnable() {
            @Override
            public void run() {
                QCoreApplication.exit(0);
            }
        });
    }

}
