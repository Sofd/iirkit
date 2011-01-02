package de.sofd.iirkit;

import de.sofd.iirkit.service.Case;
import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.log4j.Logger;

/**
 * CaseRunner#startCase starts a single case. When the case is finished,
 * the CaseRunner puts the result into its result field
 * (but doesn't update the database), and notifies all the registered
 * caseFinishedListeners.
 *
 * @author olaf
 */
public class CaseRunner implements BRContext {

    static final Logger logger = Logger.getLogger(CaseRunner.class);

    private Case currentCase;
    private final List<ChangeListener> caseFinishedListeners = new LinkedList<ChangeListener>();
    private App app;

    //all frames here...
    BRFrameView frame0;
    JFrame formFrame;

    public CaseRunner(App app) {
        this.app = app;
    }

    @Override
    public Case getCurrentCase() {
        return currentCase;
    }

    public void startCase(Case c) {
        logger.info("starting case: " + c);
        if (!(frame0 != null && frame0.getFrame().isVisible())) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] gs = ge.getScreenDevices();

            frame0 = new BRFrameView(app, gs[0].getDefaultConfiguration(), 0);
            frame0.getFrame().setSize(800, 600);
            app.show(frame0);
            //frame0.getFrame().setVisible(true);

            formFrame = new JFrame("ECRF");
            formFrame.getContentPane().setLayout(new BorderLayout());
            JTextArea txt = new JTextArea();
            formFrame.getContentPane().add(txt, BorderLayout.CENTER);
            formFrame.getContentPane().add(new JButton(new AbstractAction("OK") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    currentCase.setResult("result " + new Date());
                    fireCaseFinished();
                }
            }), BorderLayout.SOUTH);
            formFrame.setSize(600, 600);
            app.show(formFrame);
            //formFrame.setVisible(true);
        }
        currentCase = c;
    }

    public void disposeFrames() {
        frame0.getFrame().dispose();
        formFrame.dispose();
    }

    //eventually there may be multiple kinds of finishings (e.g. ended, cancelled...)

    public void addCaseFinishedListener(ChangeListener l) {
        caseFinishedListeners.add(l);
    }

    public void removeCaseFinishedListener(ChangeListener l) {
        caseFinishedListeners.remove(l);
    }

    protected void fireCaseFinished() {
        for (ChangeListener l : caseFinishedListeners.toArray(new ChangeListener[0])) {
            l.stateChanged(new ChangeEvent(this));
        }
    }

}
