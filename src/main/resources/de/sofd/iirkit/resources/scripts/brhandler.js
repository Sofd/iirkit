var enableROIs = true;
var enableMouseMeasurements = true;
var enableLUTselection = true;
var enableLUTscale= true;

importPackage(java.lang);
importClass(java.io.File);
importClass(Packages.de.sofd.iirkit.service.SeriesGroup);
importPackage(Packages.de.sofd.viskit.controllers);
importPackage(Packages.de.sofd.viskit.controllers.cellpaint);
importPackage(Packages.de.sofd.viskit.model);
importPackage(Packages.de.sofd.viskit.ui);
importClass(Packages.de.sofd.viskit.ui.imagelist.gridlistimpl.JGridImageListView);
importClass(Packages.de.sofd.viskit.util.DicomUtil);
importClass(Packages.java.awt.BorderLayout);
importClass(Packages.java.awt.Color);
importClass(Packages.java.awt.Dimension);
importClass(Packages.java.awt.GraphicsEnvironment);
importClass(Packages.java.awt.Rectangle);
importPackage(java.awt.event);
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

function newJavaArr(javaClass) {
    var result = java.lang.reflect.Array.newInstance(javaClass, arguments.length - 1);
    //arguments is not an array :-\
    for (var i=1; i<arguments.length; i++) {
        result[i-1] = arguments[i];
    }
    return result;
}

function jsToJavaArr(jsArr, javaClass) {
    var result = java.lang.reflect.Array.newInstance(javaClass, jsArr.length);
    for (i in jsArr) {
        result[i] = jsArr[i];
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


var modelFactory = new DicomModelFactory(System.getProperty("user.home") + File.separator + "viskit-model-cache.txt", new IntuitiveFileNameComparator());
modelFactory.supportMultiframes = false;
modelFactory.checkFileReadability = false;
modelFactory.asyncMode = false;

var screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
var nScreens = screens.length;

/**
 * Called once per
 * frame and case (and thus potentially multiple times per frame, as frames
 * may be reused between cases).
 * After this method returns, the view panels for the series to be displayed will
 * be created and initializeViewPanel() will be called for each of them. After
 * this is done for all frame of the case, caseStartingPostFrameInitialization()
 * is called to finish the initialization of the case.
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
function initializeFrame(frame, frameNr, brContext) {
    print("initializeFrame");

    // default frame geometry autoconfiguration. Will work for one or more displays
    // arranged horizontally. For anything more exotic, roll your own.

    var nFrames = brContext.currentCase.hangingProtocol.seriesGroups.size();
    if (nFrames <= nScreens) {
        // frame n on screen n
        frame.frame.setBounds(screens[frameNr].defaultConfiguration.bounds);
    } else {
        //frames horizontally distributed over the whole display area
        var w = screens[nScreens-1].defaultConfiguration.bounds.maxX;
        var h = screens[nScreens-1].defaultConfiguration.bounds.maxY;
        frame.frame.setBounds(w * frameNr / nFrames, 0, w / nFrames, h);
    }
    frame.frame.title = "Window " + frameNr;
    frame.frame.defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE;
    frame.putAttribute("ui", {});
    frame.putAttribute("isInitialized", "true");
}

/**
 * The form frame for a case must be placed on the screen. This method must return
 * a java.awt.Rectangle that specifies the bounds of the frame on the screen.
 */
function getFormFrameBounds(brContext) {
    var nFrames = brContext.currentCase.hangingProtocol.seriesGroups.size();
    var b = screens[nScreens-1].defaultConfiguration.bounds;
    if (nFrames < nScreens) {
        return b;
    } else {
        return new Rectangle(b.x + b.width / 4, b.y + b.height / 4, b.width / 2, b.height / 2);
    }
}

/**
 * Called after the form for a case has just been initialized. Displaying the HTML page etc.
 * is handled internally (and has already been done when this is called),
 * so most of the time, this method doesn't have to do anything.
 */
function postInitializeForm(formRunner, brContext) {
    var nFrames = brContext.currentCase.hangingProtocol.seriesGroups.size();
    if (nFrames >= nScreens) {
        //no fewer series groups / image frames than screens => form is shown in front of an image frame,
        // as per our initializeFrame / getFormFrameBounds() code above
        // => make sure the form frame is visible
        formRunner.bringFormToFront();
    }
}

var multiSyncSetController = new MultiILVSyncSetController();
var useDynamicListsCount = System.getProperty("iirkit.useDynamicListsCount");
var useInlineEnlargedView = System.getProperty("iirkit.useInlineEnlargedView");

/**
 * Called when a view panel in a frame must be initialized. (called
 * after initializeFrame() was called for the frame that the view panel belongs to).
 * <p>
 * A view panel is the rectangular panel in a frame that normally displays a series.
 * The seriesUrl parameter is name of the directory containing the DICOM images of the series.
 * The function should usually create a list view containing those images in the panel, and possibly
 * one or more UI elements like toolbar buttons that perform operations on the view.
 *
 * You may place arbitrary data into the panel using panel.putAttribute(key,value)/getAttribute(key)
 */
function initializeViewPanel(panel, seriesUrl, brContext, frameNr, panelNr) {
    if (null == modelFactory.getModel(seriesUrl)) {
        modelFactory.addModel(seriesUrl, new File(seriesUrl));
    }
    var seriesModel = modelFactory.getModel(seriesUrl);
    if (!panel.getAttribute("ui")) {
        doInitializeViewPanel(panel, seriesModel, brContext, frameNr, panelNr);
    }
    var ui = panel.getAttribute("ui");
    ui.listView.model = seriesModel;
}


/**
 * Called when a view panel is (possibly temporarily) no longer used to display a series.
 *
 * @param panel
 * @param brContext
 */
function resetViewPanel(panel, brContext, frameNr, panelNr) {
    var ui = panel.getAttribute("ui");
    ui.listView.setModel(new DefaultListModel());
}

var infoMode = 0;

function doInitializeViewPanel(panel, seriesModel, brContext, frameNr, panelNr) {
    var ui = {};
    panel.putAttribute("ui", ui);
    var controllers = {};
    panel.putAttribute("controllers", controllers);

    panel.setLayout(new BorderLayout());
    ui.listView = new JGridImageListView();
    ui.listView.scaleMode = new JGridImageListView.MyScaleMode(1, 1);
    //ui.listView = new JGLImageListView();
    //ui.listView.scaleMode = new JGLImageListView.MyScaleMode(1, 1);

    ui.listView.background = Color.DARK_GRAY;
    panel.add(ui.listView, BorderLayout.CENTER);

    //can't directly port inner class creation w/ c'tor args -- see http://www.mail-archive.com/dev-tech-js-engine-rhino@lists.mozilla.org/msg00518.html

    controllers.lazyWindowingToOptimalInitializationController = new JavaAdapter(ImageListViewInitialWindowingController, {
        initializeCell: function(cell) {
            setWindowingToOptimal(cell);
        }
    });
    controllers.lazyWindowingToOptimalInitializationController.controlledImageListView = ui.listView;
    controllers.lazyWindowingToOptimalInitializationController.enabled = false;

    controllers.lazyWindowingToQCInitializationController = new JavaAdapter(ImageListViewInitialWindowingController, {
        initializeCell: function(cell) {
            setWindowingToQC(cell);
        }
    });
    controllers.lazyWindowingToQCInitializationController.controlledImageListView = ui.listView;
    controllers.lazyWindowingToQCInitializationController.enabled = true;

    controllers.lazyZoomPanInitializationController = new ImageListViewInitialZoomPanController(ui.listView);
    controllers.lazyZoomPanInitializationController.enabled = true;

    new ImageListViewMouseWindowingController(ui.listView);
    new ImageListViewMouseZoomPanController(ui.listView).doubleClickResetEnabled = false;
    new ImageListViewImagePaintController(ui.listView).enabled = true;
    new ImageListViewInitStateIndicationPaintController(ui.listView);
    if (enableROIs) {
        new ImageListViewRoiInputEventController(ui.listView);
        new ImageListViewRoiPaintController(ui.listView).setEnabled(true);
    }

    var sssc = new ImageListViewSelectionScrollSyncController(ui.listView);
    sssc.scrollPositionTracksSelection = true;
    sssc.selectionTracksScrollPosition = true;
    sssc.allowEmptySelection = false;
    sssc.enabled = true;

    controllers.ptc = new JavaAdapter(ImageListViewPrintTextToCellsController, {
        getTextToPrint: function(cell) {
            if (infoMode == 0) {
                return newJavaStrArr();
            }
            var elt = cell.getDisplayedModelElement();
            var dicomImageMetaData = elt.getDicomImageMetaData();
            var orientation = elt.getAttribute("orientationPreset");
            if (!orientation) {
                orientation = DicomUtil.getSliceOrientation(elt.getDicomImageMetaData());
            }
            return newJavaStrArr(
                        "SL: " + " [" + cell.getOwner().getIndexOf(cell) + "] " + dicomImageMetaData.getString(Tag.SliceLocation),
                        "O: " + orientation,
                        "WL/WW: " + cell.getWindowLocation() + "/" + cell.getWindowWidth(),
                        "Zoom: " + cell.getScale()
                        );
        }
    });
    controllers.ptc.controlledImageListView = ui.listView;
    controllers.ptc.enabled = true;

    if (enableMouseMeasurements) {
        new ImageListViewMouseMeasurementController(ui.listView).enabled = true;
    }

    var toolbar = new JToolBar();
    toolbar.floatable = false;
    panel.add(toolbar, BorderLayout.PAGE_START);

    /*
    toolbar.add(new JLabel("ScaleMode:"));
     */

    if (useInlineEnlargedView) {
        var scaleModeCombo = new JComboBox();
        ui.listView.getSupportedScaleModes().foreach(function(sm) {
            scaleModeCombo.addItem(sm);
        });
        toolbar.add(scaleModeCombo);
        scaleModeCombo.editable = false;
        Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
            ui.listView, BeanProperty.create("scaleMode"),
            scaleModeCombo, BeanProperty.create("selectedItem")).bind();
    }

    toolbar.add(createAction("wRST", "Reset Windowing",
        function() {
            controllers.wndAllController.runWithControllerInhibited(new Runnable({
                run: function() {
                    var selIdx = ui.listView.getSelectedIndex();
                    if (selIdx >= 0 && selIdx < ui.listView.getLength()) {
                        var cell = ui.listView.getCell(selIdx);
                        setWindowingToQC(cell);
                    }
                }
            }));
        }
    ));

    toolbar.add(createAction("waRST", "Reset Windowing (All Images)", function() {
        resetAllWindowing(panel);
    }));

    controllers.wndAllController = new ImageListViewWindowingApplyToAllController(ui.listView);
    controllers.wndAllController.ignoreNonInteractiveChanges = false;
    controllers.wndAllController.enabled = true;
    var wndAllCheckbox = new JCheckBox("wA");
    wndAllCheckbox.toolTipText = "Window All Images";
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
                var selIdx = ui.listView.getSelectedIndex();
                if (selIdx != -1 && ui.listView.isVisibleIndex(selIdx)) {
                    var cell = ui.listView.getCell(selIdx);
                    cell.setCenterOffset(0, 0);
                    var cellImgDisplaySize = cell.getLatestSize();
                    var cz = getUnscaledPreferredCellSize(cell);
                    var scalex = cellImgDisplaySize.width / cz.width;
                    var scaley = cellImgDisplaySize.height / cz.height;
                    cell.scale = Math.min(scalex, scaley);
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

    controllers.zpAllController = new ImageListViewZoomPanApplyToAllController(ui.listView);
    controllers.zpAllController.ignoreNonInteractiveChanges = false;
    controllers.zpAllController.enabled = true;
    var zpAllCheckbox = new JCheckBox("zA");
    zpAllCheckbox.toolTipText = "Zoom/Pan All Images";
    toolbar.add(zpAllCheckbox);
    Bindings.createAutoBinding(AutoBinding.UpdateStrategy.READ_WRITE,
            controllers.zpAllController, BeanProperty.create("enabled"),
            zpAllCheckbox, BeanProperty.create("selected")).bind();

    if (!useInlineEnlargedView) {
        //display series in separate frame?
    }

    if (enableLUTselection) {
        toolbar.add(new JLabel("lut:"));
        var lutCombo = new JComboBox();
        lutCombo.addItem("[none]");
        LookupTables.getAllKnownLuts().toArray().foreach(function(lut) {
            lutCombo.addItem(lut);
        });

        lutCombo.setRenderer(new LookupTableCellRenderer(70));
        lutCombo.addItemListener(new ItemListener() {
            itemStateChanged: function(e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    var lut = null;
                    if (lutCombo.getSelectedItem() != "[none]") {
                        lut = lutCombo.getSelectedItem();
                        //slider.setLut(lut);
                    }
                    print("activating lut: " + lut);
                    for (var i = 0; i < ui.listView.getLength(); i++) {
                        ui.listView.getCell(i).setLookupTable(lut);
                    }
                }
            }
        });
        toolbar.add(lutCombo);
    }

    if (enableLUTscale) {
        var plutc = new ImageListViewPrintLUTController(ui.listView, 4, ImageListViewPrintLUTController.ScaleType.ABSOLUTE);
        plutc.enabled = true;
    }

    ui.syncButtonsToolbar = new JToolBar();
    ui.syncButtonsToolbar.floatable = false;
    toolbar.add(ui.syncButtonsToolbar);
}



function setWindowingToOptimal(cell) {
    var usedRange = cell.getDisplayedModelElement().getImage().getUsedPixelValuesRange();
    cell.windowWidth = usedRange.getDelta();
    cell.windowLocation = (usedRange.getMin() + usedRange.getMax()) / 2;
}

function setWindowingToQC(cell) {
    //...
    setWindowingToOptimal(cell);
}

function resetAllWindowing(panel) {
    var controllers = panel.getAttribute("controllers");
    controllers.lazyWindowingToOptimalInitializationController.enabled = false;
    controllers.lazyWindowingToQCInitializationController.enabled = true;
    controllers.lazyWindowingToQCInitializationController.reset();
    controllers.wndAllController.runWithControllerInhibited(new Runnable({run:function() {
        controllers.lazyWindowingToQCInitializationController.initializeAllCellsImmediately(false);
    }}));
}


var orientations = DicomUtil.PatientBasedMainAxisOrientation.values();

/**
 * Called during the initialization of a case after all the frames and all the panels
 * inside them and the form frame have been initialized.
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
            result.enabled = true;
            return result;
        }
    }));
    multiSyncSetController.addSyncControllerType("zoompan", new JavaAdapter(MultiILVSyncSetController.SyncControllerFactory, {
        createController: function() {
            var result = new GenericILVCellPropertySyncController(newJavaStrArr("scale", "centerOffset"));
            result.enabled = true;
            return result;
        }
    }));

    // initialize synchronizations
    orientations.foreach(function(o) {
        multiSyncSetController.getSyncSet(o).syncController("selection", true);
        multiSyncSetController.getSyncSet(o).syncController("windowing", true);
        multiSyncSetController.getSyncSet(o).syncController("zoompan", true);
    });

    var seriesGroups = brContext.getCurrentCase().getHangingProtocol().getSeriesGroups().toArray();
    var frames = brContext.getCurrentCaseFrames().toArray();
    //there is one frame per series group; the frames correspond 1:1 to the seriesGroups

    frames.foreach(function(frame) {
        var frameUi = frame.getAttribute("ui");
        frameUi.listViews = [];
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
                ui.orientation = orientation;
            }
            frameUi.listViews.push(ui.listView);
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
                        cb.toolTipText = "Synchronize this series";
                        ui.syncButtonsToolbar.add(cb);
                        cb.selected = true;
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
            infoMode = (infoMode + 1) % 2;
            frames.foreach(function(frame) {
                frame.activeViewPanels.toArray().foreach(function(vp) {
                    vp.getAttribute("ui").listView.refreshCells();
                });
            });
        }));
        frameView.mainToolBar.addSeparator();
        if (enableROIs) {
            var frameUi = frameView.getAttribute("ui");
            var roiToolPanel = new RoiToolPanel();
            frameView.mainToolBar.add(roiToolPanel);
            new ImageListViewRoiToolApplicationController(frameUi.listViews).setRoiToolPanel(roiToolPanel);
            frameView.mainToolBar.addSeparator();
        }
        frameView.mainToolBar.add(new JLabel("Sync: "));
        orientations.foreach(function(orientation) {
            var syncSet = multiSyncSetController.getSyncSet(orientation);
            if (syncSet.getSize() > 1) {
                frameView.mainToolBar.addSeparator();
                frameView.mainToolBar.add(new JLabel("" + orientation + ": "));

                var cb = new JCheckBox("Selections");
                cb.toolTipText = "Synchronize selections between " + orientation + " series";
                cb.model = syncSet.getIsControllerSyncedModel("selection");
                frameView.mainToolBar.add(cb);

                cb = new JCheckBox("Windowing");
                cb.toolTipText = "Synchronize windowing between " + orientation + " series";
                cb.model = syncSet.getIsControllerSyncedModel("windowing");
                frameView.mainToolBar.add(cb);

                cb = new JCheckBox("Zoom/Pan");
                cb.toolTipText = "Synchronize zoom/pan settings between " + orientation + " series";
                cb.model = syncSet.getIsControllerSyncedModel("zoompan");
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
function frameDisposing(frame, frameNr, brContext) {
}

/**
 * Called when the user submitted
 * the eCRF form to finish a case. formResult is already written to
 * brContext.currentCase.result.
 */
function caseFinished(brContext) {
}

function getSuperadminPassword() {
    return "iirgod";
}
