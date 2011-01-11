package de.sofd.iirkit;

import de.sofd.iirkit.form.FormDoneEvent;
import de.sofd.iirkit.form.FormDoneListener;
import de.sofd.iirkit.form.FormRunner;
import de.sofd.iirkit.service.Case;
import de.sofd.iirkit.service.HangingProtocol;
import de.sofd.iirkit.service.SeriesGroup;
import de.sofd.viskit.model.DicomModelFactory;
import de.sofd.viskit.model.IntuitiveFileNameComparator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
    private final List<ChangeListener> caseFinishedListeners = new LinkedList<ChangeListener>(); //TODO: specific "CaseDoneEvent"
    private final BRHandler brHandler = new BRHandler();
    private final App app;

    private final DicomModelFactory modelFactory;

    //all frames here...
    private final List<BRFrameView> frames = new ArrayList<BRFrameView>();
    private final FormRunner formRunner;

    public CaseRunner(App app) {
        this.app = app;
        formRunner = new FormRunner(app);
        modelFactory = new DicomModelFactory(System.getProperty("user.home") + File.separator + "viskit-model-cache.txt", new IntuitiveFileNameComparator());
        modelFactory.setSupportMultiframes(false);
        modelFactory.setCheckFileReadability(false);
        modelFactory.setAsyncMode(false);
    }

    @Override
    public Case getCurrentCase() {
        return currentCase;
    }

    @Override
    public List<BRFrameView> getCurrentCaseFrames() {
        return Collections.unmodifiableList(frames);
    }

    public void startCase(Case c) {
        logger.info("starting case: " + c);
        currentCase = c;
        brHandler.caseStarting(this);
        initializeFramesFor(c);
        initializeFormFrameFor(c);
        brHandler.caseStartingPostFrameInitialization(this);
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
        formRunner.start(c.getHangingProtocolObject().getEcrfUrl());
        formRunner.addFormDoneListener(new FormDoneListener() {
            @Override
            public void formSubmitted(FormDoneEvent event) {
                formRunner.removeFormDoneListener(this);
                currentCase.setResult(event.getFormResult());
                brHandler.caseFinished(CaseRunner.this);
                fireCaseFinished();
            }

            @Override
            public void formCancelled(FormDoneEvent event) {
                formRunner.removeFormDoneListener(this);
                //TODO: send specific CaseDoneEvent or similar indicating the case was cancelled
                System.exit(0);
            }
        });
        brHandler.initializeFormFrame(formRunner.getFormFrame(), this);
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
            brHandler.initializeViewPanel(vp, modelFactory.getModel(serUrl), this);
        }
        //reset model in unused lists
        for (; i < frame.getViewPanelsCount(); i++) {
            brHandler.resetViewPanel(frame.getViewPanel(i), this);
        }
        frame.setDisplayRange(0, serGrp.getSeriesUrlsCount() - 1);
        frame.setActiveViewPanelsCount(serGrp.getSeriesUrlsCount());
    }

    public void disposeFrames() {
        for (BRFrameView frame : frames) {
            frame.getFrame().dispose();
        }
        frames.clear();
        formRunner.disposeFrame();
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
