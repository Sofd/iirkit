package de.sofd.iirkit.form;

import com.trolltech.qt.core.QCoreApplication;
import com.trolltech.qt.gui.QApplication;
import de.sofd.iirkit.App;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author olaf
 */
public class FormRunner {

    private boolean isRunning = false;
    private final List<ChangeListener> finishedListeners = new ArrayList<ChangeListener>(); //TODO: use specific event + listener class
    private FormFrame formFrame;
    private String lastFormResult;
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

    public void start(String url) {
        if (isRunning) {
            throw new IllegalStateException("FormRunner already running");
        }
        QApplication.invokeLater(new Runnable() {
            @Override
            public void run() {
                formFrame = new FormFrame();
                formFrame.setMainWindowCloseCallback(new Runnable() {
                    @Override
                    public void run() {
                        formFrame.setMainWindowCloseCallback(null);
                        try {
                            SwingUtilities.invokeAndWait(new Runnable() {

                                @Override
                                public void run() {
                                    lastFormResult = "result " + new Date();
                                    System.err.println("Form result created: " + lastFormResult);
                                    stop();
                                    fireFinished();
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

    public void addFinishedListener(ChangeListener l) {
        finishedListeners.add(l);
    }

    public void removeFinishedListener(ChangeListener l) {
        finishedListeners.remove(l);
    }

    protected void fireFinished() {
        for (ChangeListener l : finishedListeners.toArray(new ChangeListener[0])) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

    public String getLastFormResult() {
        return lastFormResult;
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
