package de.sofd.iirkit;

import de.sofd.iirkit.service.SeriesGroup;
import de.sofd.viskit.controllers.GenericILVCellPropertySyncController;
import de.sofd.viskit.controllers.ImageListViewSelectionSynchronizationController;
import de.sofd.viskit.controllers.MultiILVSyncSetController;
import de.sofd.viskit.controllers.MultiImageListViewController;
import de.sofd.viskit.model.DicomImageListViewModelElement;
import de.sofd.viskit.ui.imagelist.ImageListView;
import de.sofd.viskit.util.DicomUtil;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import static de.sofd.viskit.util.DicomUtil.PatientBasedMainAxisOrientation;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
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

    private final MultiILVSyncSetController multiSyncSetController = new MultiILVSyncSetController();

    /**
     * Called when a new case is being started, before the frames are created/initialized.
     * brContext.getCurrentCaseFrames() doesn't contain valid values.
     *
     * @param brContext
     */
    void caseStarting(BRContext brContext) {
    }

    /**
     * Called once per
     * frame and case (and thus potentially multiple times per frame, as frames
     * may be reused between cases). The method should place and
     * intialize the frame (not the listViews inside it)
     */
    void initializeFrame(BRFrameView frame, int frameNo, BRContext brContext) {
        frame.getFrame().setSize(800, 600);
    }

    void initializeFormFrame(JFrame formFrame, BRContext brContext) {
        formFrame.setSize(600, 600);
    }

    void initializeViewPanel(BRViewPanel panel, ListModel/*or ModelFactory+key?*/ seriesModel) {

    }

    /**
     * Called when a new case is being started, after the frames (and the panels inside them)
     * are created/initialized. brContext.getCurrentCaseFrames() contains the frames.
     *
     * @param brContext
     */
    void caseStartingPostFrameInitialization(BRContext brContext) {
        multiSyncSetController.disconnect();
        //multiSyncSetController.addSyncSet(DicomUtil.PatientBasedMainAxisOrientation.CORONAL);
        //multiSyncSetController.addSyncSet(DicomUtil.PatientBasedMainAxisOrientation.SAGGITAL);
        //multiSyncSetController.addSyncSet(DicomUtil.PatientBasedMainAxisOrientation.TRANSVERSAL);
        for (DicomUtil.PatientBasedMainAxisOrientation orientation : DicomUtil.PatientBasedMainAxisOrientation.values()) {
            multiSyncSetController.addSyncSet(orientation);
        }
        multiSyncSetController.addSyncControllerType("selection", new MultiILVSyncSetController.SyncControllerFactory() {

            @Override
            public MultiImageListViewController createController() {
                ImageListViewSelectionSynchronizationController result = new ImageListViewSelectionSynchronizationController();
                result.setKeepRelativeSelectionIndices(true);
                result.setEnabled(true);
                return result;
            }
        });
        multiSyncSetController.addSyncControllerType("windowing", new MultiILVSyncSetController.SyncControllerFactory() {

            @Override
            public MultiImageListViewController createController() {
                GenericILVCellPropertySyncController result = new GenericILVCellPropertySyncController(new String[]{"windowLocation", "windowWidth"});
                result.setEnabled(true);
                return result;
            }
        });
        multiSyncSetController.addSyncControllerType("zoompan", new MultiILVSyncSetController.SyncControllerFactory() {

            @Override
            public MultiImageListViewController createController() {
                GenericILVCellPropertySyncController result = new GenericILVCellPropertySyncController(new String[]{"scale", "centerOffset"});
                result.setEnabled(true);
                return result;
            }
        });
        // initialize synchronizations
        for (DicomUtil.PatientBasedMainAxisOrientation orientation : DicomUtil.PatientBasedMainAxisOrientation.values()) {
            multiSyncSetController.getSyncSet(orientation).syncController("selection", true);
            multiSyncSetController.getSyncSet(orientation).syncController("windowing", true);
            multiSyncSetController.getSyncSet(orientation).syncController("zoompan", true);
        }

        final List<SeriesGroup> seriesGroups = brContext.getCurrentCase().getHangingProtocolObject().getSeriesGroups();
        final List<BRFrameView> frames = brContext.getCurrentCaseFrames();
        //there is one frame per series group; the frames correspond 1:1 to the seriesGroups

        for (BRFrameView frame : frames) {
            for (BRViewPanel vp : frame.getActiveViewPanels()) {
                final ImageListView listView = vp.getListView();
                if (listView.getLength() > 0) {
                    DicomImageListViewModelElement elt = (DicomImageListViewModelElement) listView.getElementAt(0);
                    PatientBasedMainAxisOrientation orientation = (PatientBasedMainAxisOrientation) elt.getAttribute("orientationPreset");
                    if (orientation == null) {
                        orientation = DicomUtil.getSliceOrientation(elt.getDicomImageMetaData());
                    }
                    if (orientation != null) {
                        final MultiILVSyncSetController.SyncSet syncSet = multiSyncSetController.getSyncSet(orientation);
                        syncSet.addList(listView);
                    }
                }
            }
        }

        for (BRFrameView frame : frames) {
            for (BRViewPanel vp : frame.getActiveViewPanels()) {
                final ImageListView listView = vp.getListView();
                vp.getSyncButtonsToolbar().removeAll();
                if (listView.getLength() > 0) {
                    DicomImageListViewModelElement elt = (DicomImageListViewModelElement) listView.getElementAt(0);
                    PatientBasedMainAxisOrientation orientation = (PatientBasedMainAxisOrientation) elt.getAttribute("orientationPreset");
                    if (orientation == null) {
                        orientation = DicomUtil.getSliceOrientation(elt.getDicomImageMetaData());
                    }
                    if (orientation != null) {
                        final MultiILVSyncSetController.SyncSet syncSet = multiSyncSetController.getSyncSet(orientation);
                        if (syncSet.getSize() > 1) {
                            final JCheckBox cb = new JCheckBox("Sync");
                            cb.setToolTipText("Synchronize this series");
                            vp.getSyncButtonsToolbar().add(cb);
                            cb.setSelected(true);
                            cb.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    if (cb.isSelected()) {
                                        syncSet.addList(listView);
                                    } else {
                                        syncSet.removeList(listView);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }

        for (BRFrameView frameView : brContext.getCurrentCaseFrames()) {
            frameView.mainToolBar.removeAll();
            frameView.mainToolBar.add(new AbstractAction("Info") {

                @Override
                public void actionPerformed(ActionEvent e) {
                    for (BRFrameView frame : frames) {
                        for (BRViewPanel vp : frame.getActiveViewPanels()) {
                            vp.toggleInfo();
                        }
                    }
                }
            });
            frameView.mainToolBar.addSeparator();
            frameView.mainToolBar.add(new JLabel("Sync: "));
            for (DicomUtil.PatientBasedMainAxisOrientation orientation : DicomUtil.PatientBasedMainAxisOrientation.values()) {
                final MultiILVSyncSetController.SyncSet syncSet = multiSyncSetController.getSyncSet(orientation);
                if (syncSet.getSize() < 2) {
                    continue;
                }

                frameView.mainToolBar.addSeparator();
                frameView.mainToolBar.add(new JLabel("" + orientation + ": "));

                JCheckBox cb = new JCheckBox("Selections");
                cb.setToolTipText("Synchronize selections between " + orientation + " series");
                cb.setModel(syncSet.getIsControllerSyncedModel("selection"));
                frameView.mainToolBar.add(cb);

                cb = new JCheckBox("Windowing");
                cb.setToolTipText("Synchronize windowing between " + orientation + " series");
                cb.setModel(syncSet.getIsControllerSyncedModel("windowing"));
                frameView.mainToolBar.add(cb);

                cb = new JCheckBox("Zoom/Pan");
                cb.setToolTipText("Synchronize zoom/pan settings between " + orientation + " series");
                cb.setModel(syncSet.getIsControllerSyncedModel("zoompan"));
                frameView.mainToolBar.add(cb);
            }
            frameView.getFrame().invalidate();
            frameView.getFrame().validate();
            frameView.getFrame().repaint();
        }
    }

    void frameDisposing(BRFrameView frame, int frameNo, BRContext brContext) {
    }

    /**
     * OWC calls this when it recognizes that the user clicked OK on
     * the form. formResult already written to
     * brContext.currentCase.result?
     */
    void caseFinished(BRContext brContext) {

    }

}
