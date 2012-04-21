package de.sofd.iirkit.form;

import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.gui.QApplication;
import de.sofd.iirkit.App;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.swing.SwingUtilities;
import org.apache.log4j.Logger;

/**
 * Usage: Create a FormRunner, register an event listener that is called when
 * the runner is finished, call FormRunner#start(formUrl).
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
 * @author olaf
 */
public class FormRunner {

    static final Logger logger = Logger.getLogger(FormRunner.class);

    private boolean isRunning = false;
    private final List<FormDoneListener> finishedListeners = new ArrayList<FormDoneListener>();
    private Runnable formShownCallback;
    private FormFrame formFrame;
    private final App app;

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

    public void start(final String url) {
        start(url, null, null);
    }

    public void start(final String url, Rectangle formBounds) {
        start(url, formBounds, null);
    }

    public void start(final String url, final Rectangle formBounds, final String formContents) {
        if (isRunning) {
            throw new IllegalStateException("FormRunner already running");
        }
        QApplication.invokeLater(new Runnable() {
            @Override
            public void run() {
                formFrame = new FormFrame(url, formContents);
                formFrame.setFormDoneCallback(new Runnable() {
                    @Override
                    public void run() {
                        final FormDoneEvent formDoneEvent = formFrame.getFormDoneEvent();
                        formFrame.setFormDoneCallback(null);
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {

                                @Override
                                public void run() {
                                    stop();
                                    fireFinished(formDoneEvent);
                                }
                            });
                        } catch (Exception ex) {
                            throw new RuntimeException("qt invocation failed: " + ex.getLocalizedMessage(), ex);
                        }
                    }
                });
                formFrame.show();
                if (null != formBounds) {
                    formFrame.setGeometry(formBounds.x, formBounds.y, formBounds.width, formBounds.height);
                }
                if (formShownCallback != null) {
                    formShownCallback.run();
                }
            }
        });

        isRunning = true;
    }

    public FormFrame getFormFrame() {
        return formFrame;
    }

    public void addFormDoneListener(FormDoneListener l) {
        finishedListeners.add(l);
    }

    public void removeFormDoneListener(FormDoneListener l) {
        finishedListeners.remove(l);
    }

    protected void fireFinished(FormDoneEvent evt) {
        for (FormDoneListener l : finishedListeners.toArray(new FormDoneListener[0])) {
            if (evt.isFormSubmitted()) {
                l.formSubmitted(evt);
            } else {
                l.formCancelled(evt);
            }
        }
    }

    public Runnable getFormShownCallback() {
        return formShownCallback;
    }

    /**
     * Callback to run when the form frame was just shown. RUNS IN THE QT THREAD.
     *
     * @param formShownCallback
     */
    public void setFormShownCallback(Runnable formShownCallback) {
        this.formShownCallback = formShownCallback;
    }

    public void runJavascriptInFormAsync(final String jsCode) {
        //TODO: synchronize with form loading
        QApplication.invokeLater(new Runnable() {
            @Override
            public void run() {
                formFrame.runJavascriptInForm(jsCode);
            }
        });
    }

    public void disposeFrame() {
        QApplication.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (null != formFrame) {
                    formFrame.close();
                }
            }
        });
    }

    public void cancel() {
        fireFinished(new FormDoneEvent());
        stop();
    }

    protected void stop() {
        //QApplication.invokeAndWait would probably lead to a deadlock when stop()
        //is called from a Swing.invokeAndWait(). Might invokeLater() in theory
        //cause a race condition?
        QApplication.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (null != formFrame) {
                    logger.debug("async formFrame.close() as a result of a FormRunner#stop...");
                    formFrame.close();
                }
            }
        });
        //formFrame.dispose();
        isRunning = false;
        //TODO: fireFinished() unless our caller already did
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
