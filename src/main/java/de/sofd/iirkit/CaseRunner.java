package de.sofd.iirkit;

import com.google.common.collect.Multimap;
import de.sofd.iirkit.form.FormAdapter;
import de.sofd.iirkit.form.FormEvent;
import de.sofd.iirkit.form.FormListener;
import de.sofd.iirkit.form.FormRunner;
import de.sofd.iirkit.service.Case;
import de.sofd.iirkit.service.HangingProtocol;
import de.sofd.iirkit.service.SeriesGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;

/**
 * CaseRunner#openCase starts a single case, bringing up image and
 * form frames as needed. When the case is finished,
 * the CaseRunner puts the result into its result field
 * (but doesn't update the database), and notifies all the registered
 * caseFinishedListeners.
 *
 * @author Olaf Klischat
 */
public class CaseRunner implements BRContext {

    static final Logger logger = Logger.getLogger(CaseRunner.class);

    private Case currentCase;
    private boolean showPreviousResult, readOnly;
    private final List<CaseListener> caseListeners = new LinkedList<CaseListener>();
    private final BRHandler brHandler;
    private final App app;

    //all frames here...
    private final List<BRFrameView> frames = new ArrayList<BRFrameView>();
    private final FormRunner formRunner;

    public CaseRunner(App app, BRHandler brHandler) {
        this.app = app;
        this.brHandler = brHandler;
        formRunner = new FormRunner(app);
        formRunner.addFormListener(formListener);
        BRHandler.setFormRunnerForEcrfJavascript(formRunner);
    }

    @Override
    public Case getCurrentCase() {
        return currentCase;
    }

    public boolean isCaseRunning() {
        return currentCase != null;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public boolean isShowPreviousResult() {
        return showPreviousResult;
    }

    @Override
    public List<BRFrameView> getCurrentCaseFrames() {
        return Collections.unmodifiableList(frames);
    }
    
    @Override
    public FormRunner getFormRunner() {
    	return formRunner;
    }

    public void openCase(Case c) {
        openCase(c, false, false);
    }

    public void openCase(Case c, boolean showPreviousResult, boolean readOnly) {
        this.currentCase = c;
        this.showPreviousResult = showPreviousResult;
        this.readOnly = readOnly;
        brHandler.caseStarting(this);
        initializeFramesFor(c);
        initializeFormFrameFor(c);
        brHandler.caseStartingPostFrameInitialization(this);
    }

    protected void initializeFramesFor(Case c) {
        HangingProtocol hp = c.getHangingProtocol();
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
        formRunner.openForm(c.getHangingProtocol().getEcrfUrl(), brHandler.getFormFrameBounds(this), isShowPreviousResult() ? c.getResult() : null);
        formRunner.setFormEnabled(!readOnly);
        brHandler.postInitializeForm(formRunner, CaseRunner.this);
    }

    private FormListener formListener = new FormAdapter() {
        @Override
        public void formSubmitted(FormEvent event) {
            if (!isReadOnly()) {
                currentCase.setResult(event.getFormResult());
            }
            brHandler.caseFinished(CaseRunner.this);
            fireCaseSubmitted(event.getFormResultMap());
        }
        @Override
        public void formDeleted(FormEvent event) {
            closeCase();
        }
    };

    protected void initializeViewPanelsFor(Case c, BRFrameView frame, int frameNr) {
        HangingProtocol hp = c.getHangingProtocol();
        SeriesGroup serGrp = hp.getSeriesGroups().get(frameNr);
        int i;
        //create new lists if necessary (we never delete once-created lists, only remove them from the layout)
        for (i = 0; i < serGrp.getSeriesUrlsCount(); i++) {
            if (i >= frame.getViewPanelsCount()) {
                frame.addViewPanel(new BRViewPanel(i));
            }
            BRViewPanel vp = frame.getViewPanel(i);
            String serUrl = serGrp.getSeriesUrl(i);
            brHandler.initializeViewPanel(vp, serUrl, this, frameNr, i);
        }
        //reset model in unused lists
        for (; i < frame.getViewPanelsCount(); i++) {
            brHandler.resetViewPanel(frame.getViewPanel(i), this, frameNr, i);
        }
        frame.setDisplayRange(0, serGrp.getSeriesUrlsCount() - 1);
        frame.setActiveViewPanelsCount(serGrp.getSeriesUrlsCount());
    }

    public void closeCase() {
        if (currentCase == null) {
            return; //avoid recursion via formListener#formClosed
        }
        currentCase = null;
        for (BRFrameView frame : frames) {
            frame.getFrame().dispose();
        }
        frames.clear();
        formRunner.deleteForm();
        fireCaseClosed();
    }

    //eventually there may be multiple kinds of finishings (e.g. ended, cancelled...)

    public void addCaseListener(CaseListener l) {
        caseListeners.add(l);
    }

    public void removeCaseListener(CaseListener l) {
        caseListeners.remove(l);
    }

    protected void fireCaseSubmitted(Multimap<String, String> requestParams) {
        if (requestParams == null) {
            throw new IllegalArgumentException("case can't finish w/ null requestParams");
        }
        for (CaseListener l : caseListeners.toArray(new CaseListener[0])) {
            l.caseSubmitted(new CaseEvent(this, requestParams));
        }
    }

    protected void fireCaseClosed() {
        for (CaseListener l : caseListeners.toArray(new CaseListener[0])) {
            l.caseClosed(new CaseEvent(this, CaseEvent.Type.CASE_CLOSED));
        }
    }

}
