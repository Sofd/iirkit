package de.sofd.iirkit;

import de.sofd.iirkit.service.Case;
import de.sofd.iirkit.service.HangingProtocol;
import de.sofd.iirkit.service.SeriesGroup;
import de.sofd.viskit.model.DicomModelFactory;
import de.sofd.viskit.model.IntuitiveFileNameComparator;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
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
    private final BRHandler brHandler = new BRHandler();
    private final App app;

    private final DicomModelFactory modelFactory;

    //all frames here...
    private final List<BRFrameView> frames = new ArrayList<BRFrameView>();
    private JFrame formFrame;

    public CaseRunner(App app) {
        this.app = app;
        modelFactory = new DicomModelFactory(System.getProperty("user.home") + File.separator + "viskit-model-cache.txt", new IntuitiveFileNameComparator());
        modelFactory.setSupportMultiframes(false);
        modelFactory.setCheckFileReadability(false);
        modelFactory.setAsyncMode(false);
    }

    @Override
    public Case getCurrentCase() {
        return currentCase;
    }

    public void startCase(Case c) {
        logger.info("starting case: " + c);
        currentCase = c;
        initializeFramesFor(c);
        initializeFormFrameFor(c);
    }

    protected void initializeFramesFor(Case c) {
        HangingProtocol hp = c.getHangingProtocolObject();
        int nExistingFrames = frames.size();
        int nSerGrps = hp.getSeriesGroups().size();
        //create/dispose frames as necessary to match up
        //the frames count with the series group count
        if (nExistingFrames < nSerGrps) {
            for (int frameNo = nExistingFrames; frameNo < nSerGrps; frameNo++) {
                BRFrameView frame = new BRFrameView(app, 0);
                app.show(frame);
                brHandler.initializeFrame(frame, frameNo, this);  //called again below -- OK?
                frames.add(frame);
            }
        } else {
            for (int frameNo = nExistingFrames - 1; frameNo >= nSerGrps; frameNo--) {
                BRFrameView frame = frames.get(frameNo);
                brHandler.frameDisposing(frame, frameNo, this);
                frame.getFrame().dispose();
                frames.remove(frameNo);
            }
        }

        for (int frameNo = 0; frameNo < nSerGrps; frameNo++) {
            SeriesGroup serGrp = hp.getSeriesGroups().get(frameNo);
            BRFrameView frame = frames.get(frameNo);
            brHandler.initializeFrame(frame, frameNo, this);
            initializeViewPanelsFor(c, frame, frameNo);
        }
    }

    protected void initializeFormFrameFor(Case c) {
        if (formFrame == null) {
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
        }
        brHandler.initializeFormFrame(formFrame, this);
    }

    protected void initializeViewPanelsFor(Case c, BRFrameView frame, int frameNo) {
        HangingProtocol hp = c.getHangingProtocolObject();
        SeriesGroup serGrp = hp.getSeriesGroups().get(frameNo);
        int i;
        //create new lists if necessary (we never delete once-created lists, only remove them from the layout)
        for (i = 0; i < serGrp.getSeriesUrlsCount(); i++) {
            if (i >= frame.getViewPanelsCount()) {
                frame.addViewPanel(new BRViewPanel(i));
            }
            BRViewPanel vp = frame.getViewPanel(i);
            String serUrl = serGrp.getSeriesUrl(i);
            if (null == modelFactory.getModel(serUrl)) {
                modelFactory.addModel(serUrl, new File(serUrl));
            }
            vp.getListView().setModel(modelFactory.getModel(serUrl));
        }
        //reset model in unused lists
        for (; i < frame.getViewPanelsCount(); i++) {
            frame.getViewPanel(i).getListView().setModel(new DefaultListModel());
        }
        frame.setDisplayRange(0, serGrp.getSeriesUrlsCount() - 1);
    }

    public void disposeFrames() {
        for (BRFrameView frame : frames) {
            frame.getFrame().dispose();
        }
        frames.clear();
        formFrame.dispose();
        formFrame = null;
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
