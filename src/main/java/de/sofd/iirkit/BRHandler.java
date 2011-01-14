package de.sofd.iirkit;

import de.sofd.iirkit.form.FormFrame;
import de.sofd.iirkit.service.SeriesGroup;
import de.sofd.lang.Function2;
import de.sofd.lang.Runnable2;
import de.sofd.util.FloatRange;
import de.sofd.viskit.controllers.GenericILVCellPropertySyncController;
import de.sofd.viskit.controllers.ImageListViewInitialWindowingController;
import de.sofd.viskit.controllers.ImageListViewInitialZoomPanController;
import de.sofd.viskit.controllers.ImageListViewMouseMeasurementController;
import de.sofd.viskit.controllers.ImageListViewMouseWindowingController;
import de.sofd.viskit.controllers.ImageListViewMouseZoomPanController;
import de.sofd.viskit.controllers.ImageListViewRoiInputEventController;
import de.sofd.viskit.controllers.ImageListViewSelectionScrollSyncController;
import de.sofd.viskit.controllers.ImageListViewSelectionSynchronizationController;
import de.sofd.viskit.controllers.ImageListViewWindowingApplyToAllController;
import de.sofd.viskit.controllers.ImageListViewZoomPanApplyToAllController;
import de.sofd.viskit.controllers.MultiILVSyncSetController;
import de.sofd.viskit.controllers.MultiImageListViewController;
import de.sofd.viskit.controllers.cellpaint.ImageListViewImagePaintController;
import de.sofd.viskit.controllers.cellpaint.ImageListViewPrintTextToCellsController;
import de.sofd.viskit.model.DicomImageListViewModelElement;
import de.sofd.viskit.model.ImageListViewModelElement;
import de.sofd.viskit.ui.imagelist.ImageListViewCell;
import de.sofd.viskit.ui.imagelist.JImageListView;
import de.sofd.viskit.ui.imagelist.glimpl.JGLImageListView;
import de.sofd.viskit.ui.imagelist.gridlistimpl.JGridImageListView;
import de.sofd.viskit.util.DicomUtil;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import static de.sofd.viskit.util.DicomUtil.PatientBasedMainAxisOrientation;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.WindowConstants;
import org.apache.log4j.Logger;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

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

    static final Logger logger = Logger.getLogger(BRHandler.class);

    private Scriptable jsScope;
    private boolean isJsInitialized = false;

    /**
     * For use from Javascript.
     * 
     * @param s
     */
    public static void print(String s) {
        System.out.println(s);
    }

    /**
     * Run code with a valid Rhine context and scope (and the brhandler.js script
     * loaded). Return what the code returned.
     *
     * @param code
     */
    private Object runInRhinoContext(Function2<Context, Scriptable, Object> code) {
        Context cx = Context.enter();
        try {
            if (null == jsScope) {
                ScriptableObject jsScopeTmp = cx.initStandardObjects();
                if (!isJsInitialized) {
                    try {
                        jsScopeTmp.defineFunctionProperties(new String[]{"print"}, BRHandler.this.getClass(), ScriptableObject.DONTENUM);
                        Reader r = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("de/sofd/iirkit/resources/scripts/brhandler.js"), "utf-8");
                        cx.evaluateReader(jsScopeTmp, r, "brHandler", 1, null);
                        //cx.evaluateString(jsScopeTmp, "print('HELLO FROM JS'); function caseStarting(ctx) { print('CASE STARTING'); }", "<cmd>", 1, null);
                    } catch (IOException ex) {
                        throw new RuntimeException("I/O error reading the brHandler script: " + ex.getLocalizedMessage(), ex);
                    }
                    jsScope = jsScopeTmp;
                    isJsInitialized = true;
                }
            }
            return code.run(cx, jsScope);
        } finally {
            Context.exit();
        }
    }

    /**
     * Like the other variant, except the code (and thus this function) returns
     * nothing.
     *
     * @param code
     * @return
     */
    private void runInRhinoContext(final Runnable2<Context, Scriptable> code) {
        runInRhinoContext(new Function2<Context, Scriptable, Object>() {
            @Override
            public Object run(Context p0, Scriptable p1) {
                code.run(p0, p1);
                return null;
            }
        });
    }

    private Object callJsFunction(final String name, final Object... args) {
        return runInRhinoContext(new Function2<Context, Scriptable, Object>() {
            @Override
            public Object run(Context cx, Scriptable scope) {
                Object fn = scope.get(name, scope);
                if (!(fn instanceof Function)) {
                    logger.debug("function not defined in script: caseStarting");
                    return null;
                } else {
                    return ((Function)fn).call(cx, scope, scope, args);
                }
            }
        });
    }

    private final MultiILVSyncSetController multiSyncSetController = new MultiILVSyncSetController();

    private boolean useDynamicListsCount = (null != System.getProperty("iirkit.useDynamicListsCount"));
    private boolean useJ2DInFrameViews = true; //(null != System.getProperty("iirkit.useJ2DInFrameViews"));
    private boolean useInlineEnlargedView = (null != System.getProperty("iirkit.useInlineEnlargedView"));

    private static class PanelUIElements {
        JImageListView listView;
        JToolBar syncButtonsToolbar;
    }

    private static class PanelControllers {
        ImageListViewInitialWindowingController lazyWindowingToOptimalInitializationController;
        ImageListViewInitialWindowingController lazyWindowingToQCInitializationController;
        ImageListViewInitialZoomPanController lazyZoomPanInitializationController;
        ImageListViewWindowingApplyToAllController wndAllController;
        ImageListViewZoomPanApplyToAllController zpAllController;
        ImageListViewPrintTextToCellsController ptc;
    }

    /**
     * Called when a new case is being started, before the frames are created/initialized.
     * brContext.getCurrentCaseFrames() doesn't contain valid values.
     *
     * @param brContext
     */
    void caseStarting(final BRContext brContext) {
        callJsFunction("caseStarting", brContext);
    }

    // default frame geometry autoconfiguration. Will work for multiple displays
    // arranged horizontally. For anything more exotic, roll your own.

    GraphicsDevice[] screens;

    private synchronized GraphicsDevice[] getScreens() {
        if (null == screens) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            screens = ge.getScreenDevices();
            /*
            int nScreens = 0;
            for (int j = 0; j < screens.length; j++) {
                GraphicsDevice gd = screens[j];
                GraphicsConfiguration[] gc = gd.getConfigurations();
                //for (int i = 0; i < gc.length; i++) {
                nScreens++;
                //}
            }
             */
        }
        return screens;
    }

    /**
     * Called once per
     * frame and case (and thus potentially multiple times per frame, as frames
     * may be reused between cases). The method should place and
     * intialize the frame (not the view panels/listViews inside it)
     */
    void initializeFrame(BRFrameView frame, int frameNo, BRContext brContext) {
        //if (null != frame.getAttribute("isInitialized")) {
        //    return;
        //}

        int nFrames = brContext.getCurrentCase().getHangingProtocolObject().getSeriesGroups().size();
        GraphicsDevice[] gs = getScreens();
        int nScreens = gs.length;
        if (nFrames <= nScreens) {
            // frame n on screen n
            frame.getFrame().setBounds(gs[frameNo].getDefaultConfiguration().getBounds());
        } else {
            //frames horizontally distributed over the whole display area
            int w = (int) gs[nScreens-1].getDefaultConfiguration().getBounds().getMaxX();
            int h = (int) gs[nScreens-1].getDefaultConfiguration().getBounds().getMaxY();
            frame.getFrame().setBounds(w * frameNo / nFrames, 0, w / nFrames, h);
        }

        frame.getFrame().setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.putAttribute("isInitialized", "true");
    }

    Rectangle getFormFrameBounds(BRContext brContext) {
        int nFrames = brContext.getCurrentCase().getHangingProtocolObject().getSeriesGroups().size();  //MT safe?
        //QDesktopWidget desktop = QApplication.desktop();
        //int nScreens = desktop.screenCount();  //always 1... (the whole virtual screen)
        GraphicsDevice[] gs = getScreens();
        int nScreens = gs.length;
        Rectangle b = gs[nScreens-1].getDefaultConfiguration().getBounds();
        if (nFrames < nScreens) {
            return b;
        } else {
            return new Rectangle(b.x + b.width / 4, b.y + b.height / 4, b.width / 2, b.height / 2);
        }
    }

    void initializeFormFrame(FormFrame formFrame, BRContext brContext) {
        // we're in the QT thread here
    }

    void initializeViewPanel(BRViewPanel panel, ListModel/*or ModelFactory+key?*/ seriesModel, BRContext brContext) {
        if (null == panel.getAttribute("ui")) {
            doInitializeViewPanel(panel, seriesModel);
        }
        final PanelUIElements ui = (PanelUIElements) panel.getAttribute("ui");
        ui.listView.setModel(seriesModel);
    }

    private void doInitializeViewPanel(final BRViewPanel panel, ListModel/*or ModelFactory+key?*/ seriesModel) {
        panel.setLayout(new BorderLayout());
        //panel.panelIdx = idx;
        final JImageListView listView;
        if (useJ2DInFrameViews) {
            listView = new JGridImageListView();
            listView.setScaleMode(new JGridImageListView.MyScaleMode(1, 1));
        } else {
            listView = new JGLImageListView();
            listView.setScaleMode(new JGLImageListView.MyScaleMode(1, 1));
        }

        final PanelUIElements ui = new PanelUIElements();
        panel.putAttribute("ui", ui);
        final PanelControllers controllers = new PanelControllers();
        panel.putAttribute("controllers", controllers);

        ui.listView = listView;

        listView.setBackground(Color.DARK_GRAY);
        panel.add(listView, BorderLayout.CENTER);

        controllers.lazyWindowingToOptimalInitializationController = new ImageListViewInitialWindowingController(listView) {

            @Override
            protected void initializeCell(ImageListViewCell cell) {
                setWindowingToOptimal(cell);
            }
        };
        controllers.lazyWindowingToOptimalInitializationController.setEnabled(false);

        controllers.lazyWindowingToQCInitializationController = new ImageListViewInitialWindowingController(listView) {

            @Override
            protected void initializeCell(ImageListViewCell cell) {
                setWindowingToQC(cell);
            }
        };
        controllers.lazyWindowingToQCInitializationController.setEnabled(true);

        controllers.lazyZoomPanInitializationController = new ImageListViewInitialZoomPanController(listView);
        controllers.lazyZoomPanInitializationController.setEnabled(true);

        new ImageListViewMouseWindowingController(listView);
        new ImageListViewMouseZoomPanController(listView).setDoubleClickResetEnabled(false);
        new ImageListViewRoiInputEventController(listView);
        new ImageListViewImagePaintController(listView).setEnabled(true);

        ImageListViewSelectionScrollSyncController sssc = new ImageListViewSelectionScrollSyncController(listView);
        sssc.setScrollPositionTracksSelection(true);
        sssc.setSelectionTracksScrollPosition(true);
        sssc.setAllowEmptySelection(false);
        sssc.setEnabled(true);

        controllers.ptc = new ImageListViewPrintTextToCellsController(listView) {

            @Override
            protected String[] getTextToPrint(ImageListViewCell cell) {
                Integer infoMode = (Integer) panel.getAttribute("infoMode");
                if (null == infoMode) { infoMode = 0; }
                if (infoMode == 0) {
                    return new String[0];
                }
                DicomImageListViewModelElement elt = (DicomImageListViewModelElement) cell.getDisplayedModelElement();
                DicomObject dicomImageMetaData = elt.getDicomImageMetaData();
                //"PN: " + dicomImageMetaData.getString(Tag.PatientName),
                String indexStr = "" + cell.getOwner().getIndexOf(cell);
                if (infoMode == 1) {
                    return new String[]{
                                //cellTextListArray[panelIdx],
                                //cellTextListArraySecret[panelIdx],
                                };
                } else {
                    DicomUtil.PatientBasedMainAxisOrientation orientation = (DicomUtil.PatientBasedMainAxisOrientation) elt.getAttribute("orientationPreset");
                    if (orientation == null) {
                        orientation = DicomUtil.getSliceOrientation(elt.getDicomImageMetaData());
                    }
                    return new String[]{
                                //cellTextListArray[panelIdx],
                                "SL: " + " [" + indexStr + "] " + dicomImageMetaData.getString(Tag.SliceLocation),
                                "O: " + orientation,
                                "WL/WW: " + cell.getWindowLocation() + "/" + cell.getWindowWidth(),
                                "Zoom: " + cell.getScale(),
                                //cellTextListArraySecret[panelIdx],
                                };
                }
            }
        };
        controllers.ptc.setEnabled(true);

        new ImageListViewMouseMeasurementController(listView).setEnabled(true);

        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        panel.add(toolbar, BorderLayout.PAGE_START);

        /*
        toolbar.add(new JLabel("ScaleMode:"));
         */

        if (useInlineEnlargedView) {
            final JComboBox scaleModeCombo = new JComboBox();
            for (JImageListView.ScaleMode sm : listView.getSupportedScaleModes()) {
                scaleModeCombo.addItem(sm);
            }
            toolbar.add(scaleModeCombo);
            scaleModeCombo.setEditable(false);
            Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
            listView, BeanProperty.create("scaleMode"),
            scaleModeCombo, BeanProperty.create("selectedItem")).bind();
        }

        toolbar.add(new AbstractAction("wRST") {

            {
                putValue(Action.SHORT_DESCRIPTION, "Reset Windowing");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                controllers.wndAllController.runWithControllerInhibited(new Runnable() {

                    @Override
                    public void run() {
                        int selIdx = listView.getSelectedIndex();
                        if (selIdx >= 0 && selIdx < listView.getLength()) {  //TODO: when can this be outside this range? happened once on 2010-04-12 (see log)
                            ImageListViewCell cell = listView.getCell(selIdx);
                            setWindowingToQC(cell);
                        }
                    }
                });
            }
        });

        toolbar.add(new AbstractAction("waRST") {

            {
                putValue(Action.SHORT_DESCRIPTION, "Reset Windowing (All Images)");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                resetAllWindowing(panel);
            }
        });

        controllers.wndAllController = new ImageListViewWindowingApplyToAllController(listView);
        controllers.wndAllController.setIgnoreNonInteractiveChanges(false);
        controllers.wndAllController.setEnabled(true);
        JCheckBox wndAllCheckbox = new JCheckBox("wA");
        wndAllCheckbox.setToolTipText("Window All Images");
        toolbar.add(wndAllCheckbox);
        Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
                controllers.wndAllController, BeanProperty.create("enabled"),
                wndAllCheckbox, BeanProperty.create("selected")).bind();

        toolbar.add(new AbstractAction("zRST") {

            {
                putValue(Action.SHORT_DESCRIPTION, "Reset Zoom/Pan");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                controllers.zpAllController.runWithControllerInhibited(new Runnable() {

                    @Override
                    public void run() {
                        int selIdx = listView.getSelectedIndex();
                        if (selIdx != -1 && listView.isVisibleIndex(selIdx)) {
                            ImageListViewCell cell = listView.getCell(selIdx);
                            cell.setCenterOffset(0, 0);
                            Dimension cellImgDisplaySize = cell.getLatestSize();
                            Dimension cz = getUnscaledPreferredCellSize(cell);
                            double scalex = ((double) cellImgDisplaySize.width) / cz.width;
                            double scaley = ((double) cellImgDisplaySize.height) / cz.height;
                            double scale = Math.min(scalex, scaley);
                            cell.setScale(scale);
                        }
                    }
                });
            }

            protected Dimension getUnscaledPreferredCellSize(ImageListViewCell cell) {
                int w, h;
                DicomImageListViewModelElement elt = (DicomImageListViewModelElement) cell.getDisplayedModelElement();
                w = elt.getDicomImageMetaData().getInt(Tag.Columns);
                h = elt.getDicomImageMetaData().getInt(Tag.Rows);
                return new Dimension(w, h);
            }
        });

        toolbar.add(new AbstractAction("zaRST") {

            {
                putValue(Action.SHORT_DESCRIPTION, "Reset Zoom/Pan (All Images)");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                controllers.zpAllController.runWithControllerInhibited(new Runnable() {

                    @Override
                    public void run() {
                        controllers.lazyZoomPanInitializationController.reset();
                    }
                });
            }
        });
        controllers.zpAllController = new ImageListViewZoomPanApplyToAllController(listView);
        controllers.zpAllController.setIgnoreNonInteractiveChanges(false);
        controllers.zpAllController.setEnabled(true);
        JCheckBox zpAllCheckbox = new JCheckBox("zA");
        zpAllCheckbox.setToolTipText("Zoom/Pan All Images");
        toolbar.add(zpAllCheckbox);
        Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
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

    private void setWindowingToOptimal(ImageListViewCell cell) {
        FloatRange usedRange = cell.getDisplayedModelElement().getImage().getUsedPixelValuesRange();
        cell.setWindowWidth((int) usedRange.getDelta());
        cell.setWindowLocation((int) (usedRange.getMin() + usedRange.getMax()) / 2);
    }

    private void setWindowingToQC(ImageListViewCell cell) {
        ImageListViewModelElement elt = cell.getDisplayedModelElement();
        /*
        if (elt instanceof GIImageListViewModelElement) {
            GIImageListViewModelElement gielt = (GIImageListViewModelElement) elt;
            Integer wc = gielt.getGiImage().getWindowCenter();
            Integer ww = gielt.getGiImage().getWindowWidth();
            if (wc != null && ww != null && !(wc == -1 && ww == -1)) {
                cell.setWindowLocation(wc);
                cell.setWindowWidth(ww);
            }
        }
         */
    }

    public void resetAllWindowing(BRViewPanel panel) {
        final PanelControllers controllers = (PanelControllers) panel.getAttribute("controllers");
        controllers.lazyWindowingToOptimalInitializationController.setEnabled(false);
        controllers.lazyWindowingToQCInitializationController.setEnabled(true);
        controllers.lazyWindowingToQCInitializationController.reset();
        controllers.wndAllController.runWithControllerInhibited(new Runnable() {

            @Override
            public void run() {
                controllers.lazyWindowingToQCInitializationController.initializeAllCellsImmediately(false);
            }
        });
    }

    /**
     * Called when a view panel is (possibly temporarily) no longer used to display a series.
     *
     * @param panel
     * @param brContext
     */
    void resetViewPanel(BRViewPanel panel, BRContext brContext) {
        final PanelUIElements ui = (PanelUIElements) panel.getAttribute("ui");
        ui.listView.setModel(new DefaultListModel());
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
