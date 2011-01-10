package de.sofd.iirkit.form;

import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.gui.QApplication;
import de.sofd.iirkit.App;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Usage: Create a FormRunner, register an event listener that is called when
 * the runner is finished, call FormRunner#start(formUrl).
 * <p>
 * The runner normally finishes when the user has submitted the form (in which
 * case the form result will be in {@link #getLastFormResult() }), or has
 * canceled the runner (normally by closing the form window without submitting
 * the form). The runner may also be finished externally by calling #stop().
 *
 *
 * @author olaf
 */
public class FormRunner {

    private boolean isRunning = false;
    private final List<FormDoneListener> finishedListeners = new ArrayList<FormDoneListener>();
    private FormFrame formFrame;
    private final App app;

    /**
     * There's only one QT thread that runs continuously and is shared
     * by all FormRunners (until FormRunner.dispose()). All FormRunners
     * (if there is more than one) run their QT UI in this thread.
     */
    private static Thread qtThread = new Thread() {

        @Override
        public void run() {
            QApplication.initialize(new String[0]);
            QApplication.setQuitOnLastWindowClosed(false);
            QApplication.exec();
            System.err.println("QT thread finished.");
        }

    };

    public FormRunner(App app) {
        this.app = app;
        if (!qtThread.isAlive()) {
            qtThread.start();
        }
    }

    public void start(final String url) {
        if (isRunning) {
            throw new IllegalStateException("FormRunner already running");
        }
        QApplication.invokeLater(new Runnable() {
            @Override
            public void run() {
                formFrame = new FormFrame(url);
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

    public void disposeFrame() {

    }

    public void cancel() {
        stop();
    }

    protected void stop() {
        //QApplication.invokeAndWait would probably lead to a deadlock when stop()
        //is called from a Swing.invokeAndWait(). Might invokeLater() in theory
        //cause a race condition?
        QApplication.invokeLater(new Runnable() {
            @Override
            public void run() {
                formFrame.close();
            }
        });
        if (!isRunning) {
            throw new IllegalStateException("FormRunner not running");
        }
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
