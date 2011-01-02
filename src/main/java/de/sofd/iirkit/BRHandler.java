package de.sofd.iirkit;

import javax.swing.JFrame;
import javax.swing.ListModel;

/**
 * One instance only, with global lifetime.
 *
 * Performs one-time and per-case (re)initialization of UI elements etc.
 *
 * This would eventually be implemented in JS.
 *
 * @author olaf
 */
class BRHandler {
    /**
     * Called once per
     * frame and case (and thus potentially multiple times per frame, as frames
     * may be reused between cases). The method should place and
     * intialize the frame (not the listViews inside it)
     */
    void initializeFrame(BRFrameView frame, int frameNo, BRContext brContext) {
        frame.getFrame().setSize(800, 600);
    }

    void frameDisposing(BRFrameView frame, int frameNo, BRContext brContext) {
    }

    void initializeFormFrame(JFrame formFrame, BRContext brContext) {
        formFrame.setSize(600, 600);
    }

    void initializeViewPanel(BRViewPanel panel, ListModel/*or ModelFactory+key?*/ seriesModel) {

    }

    /**
     * OWC calls this when it recognizes that the user clicked OK on
     * the form. formResult already written to
     * brContext.currentCase.result?
     */
    void finishCurrentCase(BRContext brContext, String formResult) {

    }

}
