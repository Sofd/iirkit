importPackage(java.lang);
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
importClass(Packages.javax.swing.Action);
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
importClass(Packages.org.jdesktop.beansbinding.AutoBinding);
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

function newJavaStrArr() {
    var result = java.lang.reflect.Array.newInstance(java.lang.String, arguments.length);
    //arguments is not an array :-\
    for (var i=0; i<arguments.length; i++) {
        result[i] = arguments[i];
    }
    return result;
}


function createAction(name, tooltip, callback) {
    var result = new JavaAdapter(AbstractAction, ActionListener, {
        actionPerformed: callback
    });
    result.putValue(Action.NAME, name);
    if (tooltip) {
        result.putValue(Action.SHORT_DESCRIPTION, name);
    }
    return result;
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
 * may be reused between cases).
 * After this method returns, the view panels for the series to be displayed will
 * be created and initializeViewPanel() will be called for each of them, and finally
 * caseStartingPostFrameInitialization() is called to finish the initialization of
 * the frame.
 *
 * The method should place and
 * intialize the frame (not the view panels/listViews inside it; as those will
 * be created internally), and possibly create toolbar buttons or other per-frame
 * UI elements. Note that the view panels aren't created yet when this method
 * runs; so you may want to create UI elements in caseStartingPostFrameInitialization()
 * rather than here if you need access to the view panels.
 *
 * You may place arbitrary data into the frame using frame,putAttribute(key,value)/getAttribute(key)
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
    frame.frame.setTitle("Window " + frameNo);
    frame.frame.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE;
    frame.putAttribute("isInitialized", "true");
}

/**
 * The form frame for a case must be placed on the screen. This method must return
 * a java.awt.Rectangle that specifies the bounds of the frame on the screen.
 */
function getFormFrameBounds(brContext) {
    var nFrames = brContext.currentCase.hangingProtocolObject.seriesGroups.size();
    var b = screens[nScreens-1].defaultConfiguration.bounds;
    if (nFrames < nScreens) {
        return b;
    } else {
        return new Rectangle(b.x + b.width / 4, b.y + b.height / 4, b.width / 2, b.height / 2);
    }
}

/**
 * Called when the form frame for a case is being initialized. Displaying the HTML page etc.
 * is handled internally, so most of the time, this method doesn't have to do anything.
 */
function initializeFormFrame(formFrame, brContext) {
    //runs in QT thread
}

var multiSyncSetController = new MultiILVSyncSetController();
var useDynamicListsCount = System.getProperty("iirkit.useDynamicListsCount");
var useJ2DInFrameViews = true; //java.lang.System.getProperty("iirkit.useJ2DInFrameViews");
var useInlineEnlargedView = System.getProperty("iirkit.useInlineEnlargedView");

/**
 * Called when a view panel in a frame must be initialized. (called
 * after initializeFrame() and before caseStartingPostFrameInitialization()
 * was/is called for the frame that the vie panel belongs to).
 * <p>
 * A view panel is the rectangular panel in a frame that normally displays a series.
 * The seriesModel parameter is the ListModel that holds the series data. The function
 * should usually create a view for that model in the panel, and possibly one or more
 * UI elements like toolbar buttons that perform operations on the view.
 *
 * You may place arbitrary data into the panel using panel,putAttribute(key,value)/getAttribute(key)
 */
function initializeViewPanel(panel, seriesModel, brContext) {
    if (!panel.getAttribute("ui")) {
        doInitializeViewPanel(panel, seriesModel);
    }
    panel.getAttribute("ui").listView.setModel(seriesModel);
}


/**
 * Called when a view panel is (possibly temporarily) no longer used to display a series.
 *
 * @param panel
 * @param brContext
 */
function resetViewPanel(panel, brContext) {
    var ui = panel.getAttribute("ui");
    ui.listView.setModel(new DefaultListModel());
}

function doInitializeViewPanel(panel, seriesModel) {
    panel.setLayout(new BorderLayout());
    var listView;
    if (useJ2DInFrameViews) {
        listView = new JGridImageListView();
        listView.setScaleMode(new JGridImageListView.MyScaleMode(1, 1));
    } else {
        listView = new JGLImageListView();
        listView.setScaleMode(new JGLImageListView.MyScaleMode(1, 1));
    }

    var ui = {};
    panel.putAttribute("ui", ui);
    var controllers = {};
    panel.putAttribute("controllers", controllers);

    ui.listView = listView;

    listView.setBackground(Color.DARK_GRAY);
    panel.add(listView, BorderLayout.CENTER);

    //can't directly port inner class creation w/ c'tor args -- see http://www.mail-archive.com/dev-tech-js-engine-rhino@lists.mozilla.org/msg00518.html

    controllers.lazyWindowingToOptimalInitializationController = new JavaAdapter(ImageListViewInitialWindowingController, {
        initializeCell: function(cell) {
            setWindowingToOptimal(cell);
        }
    });
    controllers.lazyWindowingToOptimalInitializationController.controlledImageListView = listView;
    controllers.lazyWindowingToOptimalInitializationController.enabled = false;

    controllers.lazyWindowingToQCInitializationController = new JavaAdapter(ImageListViewInitialWindowingController, {
        initializeCell: function(cell) {
            setWindowingToQC(cell);
        }
    });
    controllers.lazyWindowingToQCInitializationController.controlledImageListView = listView;
    controllers.lazyWindowingToQCInitializationController.enabled = true;

    controllers.lazyZoomPanInitializationController = new ImageListViewInitialZoomPanController(listView);
    controllers.lazyZoomPanInitializationController.enabled = true;

    new ImageListViewMouseWindowingController(listView);
    new ImageListViewMouseZoomPanController(listView).setDoubleClickResetEnabled(false);
    new ImageListViewRoiInputEventController(listView);
    new ImageListViewImagePaintController(listView).setEnabled(true);

    sssc = new ImageListViewSelectionScrollSyncController(listView);
    sssc.scrollPositionTracksSelection = true;
    sssc.selectionTracksScrollPosition = true;
    sssc.allowEmptySelection = false;
    sssc.enabled = true;

    controllers.ptc = new JavaAdapter(ImageListViewPrintTextToCellsController, {
        getTextToPrint: function(cell) {
            var infoMode = panel.getAttribute("infoMode");
            if (!infoMode) { infoMode = 0; }
            if (infoMode == 0) {
                return newJavaStrArr();
            }
            var elt = cell.getDisplayedModelElement();
            var dicomImageMetaData = elt.getDicomImageMetaData();
            //"PN: " + dicomImageMetaData.getString(Tag.PatientName),
            if (infoMode == 1) {
                return newJavaStrArr(
                            //cellTextListArray[panelIdx],
                            //cellTextListArraySecret[panelIdx],
                            );
            } else {
                var orientation = elt.getAttribute("orientationPreset");
                if (!orientation) {
                    orientation = DicomUtil.getSliceOrientation(elt.getDicomImageMetaData());
                }
                return newJavaStrArr(
                            //cellTextListArray[panelIdx],
                            "SL: " + " [" + cell.getOwner().getIndexOf(cell) + "] " + dicomImageMetaData.getString(Tag.SliceLocation),
                            "O: " + orientation,
                            "WL/WW: " + cell.getWindowLocation() + "/" + cell.getWindowWidth(),
                            "Zoom: " + cell.getScale()
                            //cellTextListArraySecret[panelIdx],
                            );
            }
        }
    });
    controllers.ptc.controlledImageListView = listView;
    controllers.ptc.enabled = true;

    new ImageListViewMouseMeasurementController(listView).setEnabled(true);

    var toolbar = new JToolBar();
    toolbar.floatable = false;
    panel.add(toolbar, BorderLayout.PAGE_START);

    /*
    toolbar.add(new JLabel("ScaleMode:"));
     */

    if (useInlineEnlargedView) {
        var scaleModeCombo = new JComboBox();
        listView.getSupportedScaleModes().foreach(function(sm) {
            scaleModeCombo.addItem(sm);
        });
        toolbar.add(scaleModeCombo);
        scaleModeCombo.setEditable(false);
        Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
            listView, BeanProperty.create("scaleMode"),
            scaleModeCombo, BeanProperty.create("selectedItem")).bind();
    }

    toolbar.add(createAction("wRST", "Reset Windowing",
        function() {
            controllers.wndAllController.runWithControllerInhibited(new Runnable({
                run: function() {
                    var selIdx = listView.getSelectedIndex();
                    if (selIdx >= 0 && selIdx < listView.getLength()) {
                        var cell = listView.getCell(selIdx);
                        setWindowingToQC(cell);
                    }
                }
            }));
        }
    ));

    toolbar.add(createAction("waRST", "Reset Windowing (All Images)", function() {
        resetAllWindowing(panel);
    }));

    controllers.wndAllController = new ImageListViewWindowingApplyToAllController(listView);
    controllers.wndAllController.setIgnoreNonInteractiveChanges(false);
    controllers.wndAllController.setEnabled(true);
    var wndAllCheckbox = new JCheckBox("wA");
    wndAllCheckbox.setToolTipText("Window All Images");
    toolbar.add(wndAllCheckbox);
    Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
            controllers.wndAllController, BeanProperty.create("enabled"),
            wndAllCheckbox, BeanProperty.create("selected")).bind();

    var getUnscaledPreferredCellSize = function(cell) {
        var elt = cell.getDisplayedModelElement();
        w = elt.getDicomImageMetaData().getInt(Tag.Columns);
        h = elt.getDicomImageMetaData().getInt(Tag.Rows);
        return new Dimension(w, h);
    };

    toolbar.add(createAction("zRST", "Reset Zoom/Pan", function() {
        controllers.zpAllController.runWithControllerInhibited(new Runnable({
            run: function() {
                var selIdx = listView.getSelectedIndex();
                if (selIdx != -1 && listView.isVisibleIndex(selIdx)) {
                    var cell = listView.getCell(selIdx);
                    cell.setCenterOffset(0, 0);
                    var cellImgDisplaySize = cell.getLatestSize();
                    var cz = getUnscaledPreferredCellSize(cell);
                    var scalex = cellImgDisplaySize.width / cz.width;
                    var scaley = cellImgDisplaySize.height / cz.height;
                    var scale = Math.min(scalex, scaley);
                    cell.setScale(scale);
                }
            }
        }));
     }));

    toolbar.add(createAction("zaRST", "Reset Zoom/Pan (All Images)", function() {
        controllers.zpAllController.runWithControllerInhibited(new Runnable({
            run: function() {
                controllers.lazyZoomPanInitializationController.reset();
            }
        }));
     }));

    controllers.zpAllController = new ImageListViewZoomPanApplyToAllController(listView);
    controllers.zpAllController.setIgnoreNonInteractiveChanges(false);
    controllers.zpAllController.setEnabled(true);
    var zpAllCheckbox = new JCheckBox("zA");
    zpAllCheckbox.setToolTipText("Zoom/Pan All Images");
    toolbar.add(zpAllCheckbox);
    Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
            controllers.zpAllController, BeanProperty.create("enabled"),
            zpAllCheckbox, BeanProperty.create("selected")).bind();

    if (!useInlineEnlargedView) {
        /*
        addCellMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    final ImageListViewCell sourceCell = (ImageListViewCell) e.getSource();
                    final SingleImageViewDialog sidlg = new SingleImageViewDialog(sourceCell, parentFrameView.getFrame(), true);
                    sidlg.setBounds(parentFrameView.getFrame().getBounds());
                    sidlg.setVisible(true);
                    wndAllController.runWithControllerInhibited(new Runnable() {

                        @Override
                        public void run() {
                            int[][] params = sidlg.getLastWindowingParams();
                            for (int i = 0; i < params.length; i++) {
                                int[] wndLocationAndWidth = params[i];
                                ImageListViewCell cell = listView.getCell(i);
                                cell.setWindowLocation(wndLocationAndWidth[0]);
                                cell.setWindowWidth(wndLocationAndWidth[1]);
                            }

                            // hack: wiggle the wnd. parameters of sourceCell, while also simulating an interactive (mouse-initiated) change,
                            // to make any active sync controller synchronize it over to other lists
                            int wl = sourceCell.getWindowLocation();
                            int ww = sourceCell.getWindowWidth();
                            sourceCell.setWindowLocation(wl + 1);
                            sourceCell.setInteractively("windowLocation", wl);
                            sourceCell.setWindowWidth(ww + 1);
                            sourceCell.setInteractively("windowWidth", ww);
                        }
                    });
                }
            }
        });
         *
         */
    }

    ui.syncButtonsToolbar = new JToolBar();
    ui.syncButtonsToolbar.setFloatable(false);
    toolbar.add(ui.syncButtonsToolbar);
}



function setWindowingToOptimal(cell) {
    var usedRange = cell.getDisplayedModelElement().getImage().getUsedPixelValuesRange();
    cell.setWindowWidth(usedRange.getDelta());
    cell.setWindowLocation((usedRange.getMin() + usedRange.getMax()) / 2);
}

function setWindowingToQC(cell) {
    //...
    setWindowingToOptimal(cell);
}

function resetAllWindowing(panel) {
    var controllers = panel.getAttribute("controllers");
    controllers.lazyWindowingToOptimalInitializationController.setEnabled(false);
    controllers.lazyWindowingToQCInitializationController.setEnabled(true);
    controllers.lazyWindowingToQCInitializationController.reset();
    controllers.wndAllController.runWithControllerInhibited(new Runnable({run:function() {
        controllers.lazyWindowingToQCInitializationController.initializeAllCellsImmediately(false);
    }}));
}


var orientations = DicomUtil.PatientBasedMainAxisOrientation.values();

/**
 * Called during the initialization of a frame AFTER the panels in the frame have been
 * initialized.
 */
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
    multiSyncSetController.addSyncControllerType("windowing", new JavaAdapter(MultiILVSyncSetController.SyncControllerFactory, {
        createController: function() {
            var result = new GenericILVCellPropertySyncController(newJavaStrArr("windowLocation", "windowWidth"));
            result.setEnabled(true);
            return result;
        }
    }));
    multiSyncSetController.addSyncControllerType("zoompan", new JavaAdapter(MultiILVSyncSetController.SyncControllerFactory, {
        createController: function() {
            var result = new GenericILVCellPropertySyncController(newJavaStrArr("scale", "centerOffset"));
            result.setEnabled(true);
            return result;
        }
    }));

    // initialize synchronizations
    orientations.foreach(function(o) {
        multiSyncSetController.getSyncSet(o).syncController("selection", true);
        //multiSyncSetController.getSyncSet(o).syncController("windowing", true);
        //multiSyncSetController.getSyncSet(o).syncController("zoompan", true);
    });

    var seriesGroups = brContext.getCurrentCase().getHangingProtocolObject().getSeriesGroups().toArray();
    var frames = brContext.getCurrentCaseFrames().toArray();
    //there is one frame per series group; the frames correspond 1:1 to the seriesGroups

    frames.foreach(function(frame) {
        frame.getActiveViewPanels().toArray().foreach(function(vp) {
            var ui = vp.getAttribute("ui");
            if (ui.listView.getLength() > 0) {
                var elt = ui.listView.getElementAt(0);
                var orientation = elt.getAttribute("orientationPreset");
                if (!orientation) {
                    orientation = DicomUtil.getSliceOrientation(elt.getDicomImageMetaData());
                }
                if (orientation) {
                    multiSyncSetController.getSyncSet(orientation).addList(ui.listView);
                }
            }
        });
    });

    frames.foreach(function(frame) {
        frame.getActiveViewPanels().toArray().foreach(function(vp) {
            var ui = vp.getAttribute("ui");
            ui.syncButtonsToolbar.removeAll();
            if (ui.listView.getLength() > 0) {
                var elt = ui.listView.getElementAt(0);
                var orientation = elt.getAttribute("orientationPreset");
                if (!orientation) {
                    orientation = DicomUtil.getSliceOrientation(elt.getDicomImageMetaData());
                }
                if (orientation) {
                    var syncSet = multiSyncSetController.getSyncSet(orientation);
                    if (syncSet.getSize() > 1) {
                        var cb = new JCheckBox("Sync");
                        cb.setToolTipText("Synchronize this series");
                        ui.syncButtonsToolbar.add(cb);
                        cb.setSelected(true);
                        cb.addActionListener(ActionListener({
                            actionPerformed: function() {
                                if (cb.isSelected()) {
                                    syncSet.addList(ui.listView);
                                } else {
                                    syncSet.removeList(ui.listView);
                                }
                            }
                        }));
                    }
                }
            }
        });
    });

    frames.foreach(function(frameView) {
        frameView.mainToolBar.removeAll();
        frameView.mainToolBar.add(createAction("Info", "toggle info display", function() {
            frames.foreach(function(frame) {
                frame.getActiveViewPanels().toArray().foreach(function(vp) {
                    var infoMode = vp.getAttribute("infoMode");
                    if (!infoMode) { infoMode = 0; }
                    infoMode = (infoMode + 1) % 3;
                    vp.putAttribute("infoMode", infoMode);
                    vp.getAttribute("ui").listView.refreshCells();
                });
            });
        }));
        frameView.mainToolBar.addSeparator();
        frameView.mainToolBar.add(new JLabel("Sync: "));
        orientations.foreach(function(orientation) {
            var syncSet = multiSyncSetController.getSyncSet(orientation);
            if (syncSet.getSize() > 1) {
                frameView.mainToolBar.addSeparator();
                frameView.mainToolBar.add(new JLabel("" + orientation + ": "));

                var cb = new JCheckBox("Selections");
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
        });
        frameView.getFrame().invalidate();
        frameView.getFrame().validate();
        frameView.getFrame().repaint();
    });
}


/**
 * A frame is about to be disposed.
 */
function frameDisposing(frame, frameNo, brContext) {
}

/**
 * Called this user clicked OK on
 * the form to finish a case. formResult already written to
 * brContext.currentCase.result?
 */
function caseFinished(brContext) {
}
