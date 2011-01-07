package de.sofd.iirkit;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author olaf
 */
public class FormRunner {

    private boolean isRunning = false;
    private final List<ChangeListener> finishedListeners = new ArrayList<ChangeListener>(); //TODO: use specific event + listener class
    private JFrame formFrame;
    private String lastFormResult;
    private final App app;

    public FormRunner(App app) {
        this.app = app;
    }

    public void start(String url) {
        if (isRunning) {
            throw new IllegalStateException("FormRunner already running");
        }
        formFrame = new JFrame("ECRF");
        formFrame.getContentPane().setLayout(new BorderLayout());
        JTextArea txt = new JTextArea();
        formFrame.getContentPane().add(txt, BorderLayout.CENTER);
        formFrame.getContentPane().add(new JButton(new AbstractAction("OK") {
            @Override
            public void actionPerformed(ActionEvent e) {
                lastFormResult = "result " + new Date();
                stop();
                fireFinished();
            }
        }), BorderLayout.SOUTH);
        formFrame.setSize(600, 600);
        app.show(formFrame);

        isRunning = true;
    }

    public JFrame getFormFrame() {
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
        if (!isRunning) {
            throw new IllegalStateException("FormRunner not running");
        }
        formFrame.dispose();
        isRunning = false;
    }

}
