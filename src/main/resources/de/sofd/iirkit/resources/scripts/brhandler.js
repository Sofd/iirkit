importClass(Packages.de.sofd.iirkit.form.FormFrame);
importClass(Packages.de.sofd.iirkit.service.SeriesGroup);
importClass(Packages.de.sofd.lang.Function2);
importClass(Packages.de.sofd.lang.Runnable2);
importClass(Packages.de.sofd.util.FloatRange);
importClass(Packages.de.sofd.viskit.controllers.GenericILVCellPropertySyncController);
importClass(Packages.de.sofd.viskit.controllers.ImageListViewInitialWindowingController);
importClass(Packages.de.sofd.viskit.controllers.ImageListViewInitialZoomPanController);
importClass(Packages.de.sofd.viskit.controllers.ImageListViewMouseMeasurementController);
importClass(Packages.de.sofd.viskit.controllers.ImageListViewMouseWindowingController);
importClass(Packages.de.sofd.viskit.controllers.ImageListViewMouseZoomPanController);
importClass(Packages.de.sofd.viskit.controllers.ImageListViewRoiInputEventController);
importClass(Packages.de.sofd.viskit.controllers.ImageListViewSelectionScrollSyncController);
importClass(Packages.de.sofd.viskit.controllers.ImageListViewSelectionSynchronizationController);
importClass(Packages.de.sofd.viskit.controllers.ImageListViewWindowingApplyToAllController);
importClass(Packages.de.sofd.viskit.controllers.ImageListViewZoomPanApplyToAllController);
importClass(Packages.de.sofd.viskit.controllers.MultiILVSyncSetController);
importClass(Packages.de.sofd.viskit.controllers.MultiImageListViewController);
importClass(Packages.de.sofd.viskit.controllers.cellpaint.ImageListViewImagePaintController);
importClass(Packages.de.sofd.viskit.controllers.cellpaint.ImageListViewPrintTextToCellsController);
importClass(Packages.de.sofd.viskit.model.DicomImageListViewModelElement);
importClass(Packages.de.sofd.viskit.model.ImageListViewModelElement);
importClass(Packages.de.sofd.viskit.ui.imagelist.ImageListViewCell);
importClass(Packages.de.sofd.viskit.ui.imagelist.JImageListView);
importClass(Packages.de.sofd.viskit.ui.imagelist.glimpl.JGLImageListView);
importClass(Packages.de.sofd.viskit.ui.imagelist.gridlistimpl.JGridImageListView);
importClass(Packages.de.sofd.viskit.util.DicomUtil);
importClass(Packages.java.awt.BorderLayout);
importClass(Packages.java.awt.Color);
importClass(Packages.java.awt.Dimension);
importClass(Packages.java.awt.GraphicsDevice);
importClass(Packages.java.awt.GraphicsEnvironment);
importClass(Packages.java.awt.Rectangle);
importClass(Packages.java.awt.event.ActionEvent);
importClass(Packages.java.awt.event.ActionListener);
importClass(Packages.java.io.IOException);
importClass(Packages.java.io.InputStreamReader);
importClass(Packages.java.io.Reader);
importClass(Packages.java.util.List);
importClass(Packages.javax.swing.AbstractAction);
importClass(Packages.javax.swing.Action);
importClass(Packages.javax.swing.DefaultListModel);
importClass(Packages.javax.swing.JCheckBox);
importClass(Packages.javax.swing.JComboBox);
importClass(Packages.javax.swing.JLabel);
importClass(Packages.javax.swing.JToolBar);
importClass(Packages.javax.swing.ListModel);
importClass(Packages.javax.swing.WindowConstants);
importClass(Packages.org.apache.log4j.Logger);
importClass(Packages.org.dcm4che2.data.DicomObject);
importClass(Packages.org.dcm4che2.data.Tag);
importClass(Packages.org.jdesktop.beansbinding.AutoBinding.UpdateStrategy);
importClass(Packages.org.jdesktop.beansbinding.BeanProperty);
importClass(Packages.org.jdesktop.beansbinding.Bindings);


// utils (TODO move to separate file)
if (!Array.prototype.foreach) {
    Array.prototype.foreach = function(fn) {
        for (var i=0; i<this.length; i++) {
            fn(this[i],i);
        }
    }
}

if (!Array.prototype.map) {
    Array.prototype.map = function(fn) {
        var result = new Array(this.length);
        for (var i=0; i<this.length; i++) {
            result[i] = fn(this[i],i);
        }
        return result;
    }
}


if (!Object.prototype.forEachKey) {
    Object.prototype.forEachKey = function(fn) {
        for (var key in this) {
            if (this.hasOwnProperty(key)) {
                fn(key);
            }
        }
    }
}


function caseStarting(brContext) {
    print("case starting: " + brContext.currentCase);
}


// default frame geometry autoconfiguration. Will work for one or more displays
// arranged horizontally. For anything more exotic, roll your own.

var screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
var nScreens = screens.length;

/**
 * Called once per
 * frame and case (and thus potentially multiple times per frame, as frames
 * may be reused between cases). The method should place and
 * intialize the frame (not the view panels/listViews inside it)
 */
function initializeFrame(frame, frameNo, brContext) {
    print("initializeFrame");
    var nFrames = brContext.currentCase.hangingProtocolObject.seriesGroups.size();
    if (nFrames <= nScreens) {
        // frame n on screen n
        frame.frame.setBounds(screens[frameNo].defaultConfiguration.bounds);
    } else {
        //frames horizontally distributed over the whole display area
        var w = screens[nScreens-1].defaultConfiguration.bounds.maxX;
        var h = screens[nScreens-1].defaultConfiguration.bounds.maxY;
        frame.frame.setBounds(w * frameNo / nFrames, 0, w / nFrames, h);
    }

    frame.frame.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE;
    frame.putAttribute("isInitialized", "true");
}

function getFormFrameBounds(brContext) {
    var nFrames = brContext.currentCase.hangingProtocolObject.seriesGroups.size();
    var b = screens[nScreens-1].defaultConfiguration.bounds;
    if (nFrames < nScreens) {
        return b;
    } else {
        return new Rectangle(b.x + b.width / 4, b.y + b.height / 4, b.width / 2, b.height / 2);
    }
}

var multiSyncSetController = new MultiILVSyncSetController();
var orientations = DicomUtil.PatientBasedMainAxisOrientation.values();

function caseStartingPostFrameInitialization(brContext) {
    print("caseStartingPostFrameInitialization");
    multiSyncSetController.disconnect();
    //multiSyncSetController.addSyncSet(DicomUtil.PatientBasedMainAxisOrientation.CORONAL);
    //multiSyncSetController.addSyncSet(DicomUtil.PatientBasedMainAxisOrientation.SAGGITAL);
    //multiSyncSetController.addSyncSet(DicomUtil.PatientBasedMainAxisOrientation.TRANSVERSAL);
    orientations.foreach(function(o) {
        multiSyncSetController.addSyncSet(o);
    });
    multiSyncSetController.addSyncControllerType("selection", new JavaAdapter(MultiILVSyncSetController.SyncControllerFactory, {
        createController: function() {
            var result = new ImageListViewSelectionSynchronizationController();
            result.keepRelativeSelectionIndices = true;
            result.enabled = true;
            return result;
        }
    }));
    /*
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
    */
    // initialize synchronizations
    orientations.foreach(function(o) {
        multiSyncSetController.getSyncSet(o).syncController("selection", true);
        //multiSyncSetController.getSyncSet(o).syncController("windowing", true);
        //multiSyncSetController.getSyncSet(o).syncController("zoompan", true);
    });

    /*
    final List<SeriesGroup> seriesGroups = brContext.getCurrentCase().getHangingProtocolObject().getSeriesGroups();
    final List<BRFrameView> frames = brContext.getCurrentCaseFrames();
    //there is one frame per series group; the frames correspond 1:1 to the seriesGroups

    for (BRFrameView frame : frames) {
        for (BRViewPanel vp : frame.getActiveViewPanels()) {
            final PanelUIElements ui = (PanelUIElements) vp.getAttribute("ui");
            if (ui.listView.getLength() > 0) {
                DicomImageListViewModelElement elt = (DicomImageListViewModelElement) ui.listView.getElementAt(0);
                PatientBasedMainAxisOrientation orientation = (PatientBasedMainAxisOrientation) elt.getAttribute("orientationPreset");
                if (orientation == null) {
                    orientation = DicomUtil.getSliceOrientation(elt.getDicomImageMetaData());
                }
                if (orientation != null) {
                    final MultiILVSyncSetController.SyncSet syncSet = multiSyncSetController.getSyncSet(orientation);
                    syncSet.addList(ui.listView);
                }
            }
        }
    }

    for (BRFrameView frame : frames) {
        for (BRViewPanel vp : frame.getActiveViewPanels()) {
            final PanelUIElements ui = (PanelUIElements) vp.getAttribute("ui");
            ui.syncButtonsToolbar.removeAll();
            if (ui.listView.getLength() > 0) {
                DicomImageListViewModelElement elt = (DicomImageListViewModelElement) ui.listView.getElementAt(0);
                PatientBasedMainAxisOrientation orientation = (PatientBasedMainAxisOrientation) elt.getAttribute("orientationPreset");
                if (orientation == null) {
                    orientation = DicomUtil.getSliceOrientation(elt.getDicomImageMetaData());
                }
                if (orientation != null) {
                    final MultiILVSyncSetController.SyncSet syncSet = multiSyncSetController.getSyncSet(orientation);
                    if (syncSet.getSize() > 1) {
                        final JCheckBox cb = new JCheckBox("Sync");
                        cb.setToolTipText("Synchronize this series");
                        ui.syncButtonsToolbar.add(cb);
                        cb.setSelected(true);
                        cb.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (cb.isSelected()) {
                                    syncSet.addList(ui.listView);
                                } else {
                                    syncSet.removeList(ui.listView);
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
                        Integer infoMode = (Integer) vp.getAttribute("infoMode");
                        if (null == infoMode) { infoMode = 0; }
                        infoMode = (infoMode + 1) % 3;
                        vp.putAttribute("infoMode", infoMode);
                        ((PanelUIElements) vp.getAttribute("ui")).listView.refreshCells();
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
    */
}
