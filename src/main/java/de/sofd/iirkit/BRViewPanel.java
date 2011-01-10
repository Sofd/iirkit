package de.sofd.iirkit;

import de.sofd.viskit.controllers.ImageListViewInitialWindowingController;
import de.sofd.viskit.controllers.ImageListViewMouseWindowingController;
import de.sofd.viskit.controllers.ImageListViewMouseZoomPanController;
import de.sofd.viskit.controllers.ImageListViewRoiInputEventController;
import de.sofd.viskit.controllers.ImageListViewSelectionScrollSyncController;
import de.sofd.viskit.controllers.ImageListViewWindowingApplyToAllController;
import de.sofd.viskit.controllers.ImageListViewZoomPanApplyToAllController;
import de.sofd.viskit.controllers.cellpaint.ImageListViewImagePaintController;
import de.sofd.viskit.controllers.cellpaint.ImageListViewPrintTextToCellsController;
import de.sofd.viskit.ui.imagelist.ImageListViewCell;
import de.sofd.viskit.ui.imagelist.JImageListView;
import de.sofd.viskit.ui.imagelist.glimpl.JGLImageListView;
import de.sofd.viskit.ui.imagelist.gridlistimpl.JGridImageListView;
import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import de.sofd.util.FloatRange;
import de.sofd.viskit.controllers.ImageListViewInitialZoomPanController;
import de.sofd.viskit.controllers.ImageListViewMouseMeasurementController;
import de.sofd.viskit.model.DicomImageListViewModelElement;
import de.sofd.viskit.model.ImageListViewModelElement;
import de.sofd.viskit.ui.imagelist.ImageListView;
import de.sofd.viskit.util.DicomUtil;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.jdesktop.beansbinding.AutoBinding.UpdateStrategy;
import org.jdesktop.beansbinding.BeanProperty;
import org.jdesktop.beansbinding.Bindings;

/**
 *
 * @author olaf
 */
public class BRViewPanel extends JPanel {
    //TODO: move these out
    private boolean useDynamicListsCount = (null != System.getProperty("iirkit.useDynamicListsCount"));
    private boolean useJ2DInFrameViews = true; //(null != System.getProperty("iirkit.useJ2DInFrameViews"));
    private boolean useInlineEnlargedView = (null != System.getProperty("iirkit.useInlineEnlargedView"));

    private JToolBar toolbar;
    private JToolBar syncButtonsToolbar;
    private JImageListView listView;
    //TODO: move all the controllers etc. out of here, have the BRHandler create them as needed
    //and associate them with the panel dynamically.
    private ImageListViewInitialWindowingController lazyWindowingToOptimalInitializationController;
    private ImageListViewInitialWindowingController lazyWindowingToQCInitializationController;
    private ImageListViewInitialZoomPanController lazyZoomPanInitializationController;
    private ImageListViewWindowingApplyToAllController wndAllController;
    private ImageListViewZoomPanApplyToAllController zpAllController;
    private ImageListViewPrintTextToCellsController ptc;
    private int infoMode = 1;
    private int panelIdx;
    private BRFrameView parentFrameView;
    private boolean enlarged = false;

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

    public BRViewPanel(int idx) {
        this.setLayout(new BorderLayout());
        this.panelIdx = idx;
        if (useJ2DInFrameViews) {
            listView = new JGridImageListView();
            listView.setScaleMode(new JGridImageListView.MyScaleMode(1, 1));
        } else {
            listView = new JGLImageListView();
            listView.setScaleMode(new JGLImageListView.MyScaleMode(1, 1));
        }
        listView.setBackground(Color.DARK_GRAY);
        this.add(listView, BorderLayout.CENTER);

        lazyWindowingToOptimalInitializationController = new ImageListViewInitialWindowingController(listView) {

            @Override
            protected void initializeCell(ImageListViewCell cell) {
                setWindowingToOptimal(cell);
            }
        };
        lazyWindowingToOptimalInitializationController.setEnabled(false);

        lazyWindowingToQCInitializationController = new ImageListViewInitialWindowingController(listView) {

            @Override
            protected void initializeCell(ImageListViewCell cell) {
                setWindowingToQC(cell);
            }
        };
        lazyWindowingToQCInitializationController.setEnabled(true);

        lazyZoomPanInitializationController = new ImageListViewInitialZoomPanController(listView);
        lazyZoomPanInitializationController.setEnabled(true);

        new ImageListViewMouseWindowingController(listView);
        new ImageListViewMouseZoomPanController(listView).setDoubleClickResetEnabled(false);
        new ImageListViewRoiInputEventController(listView);
        new ImageListViewImagePaintController(listView).setEnabled(true);

        ImageListViewSelectionScrollSyncController sssc = new ImageListViewSelectionScrollSyncController(listView);
        sssc.setScrollPositionTracksSelection(true);
        sssc.setSelectionTracksScrollPosition(true);
        sssc.setAllowEmptySelection(false);
        sssc.setEnabled(true);

        ptc = new ImageListViewPrintTextToCellsController(listView) {

            @Override
            protected String[] getTextToPrint(ImageListViewCell cell) {
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
        ptc.setEnabled(true);

        new ImageListViewMouseMeasurementController(listView).setEnabled(true);

        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        this.add(toolbar, BorderLayout.PAGE_START);

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
                wndAllController.runWithControllerInhibited(new Runnable() {

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
                resetAllWindowing();
            }
        });

        wndAllController = new ImageListViewWindowingApplyToAllController(listView);
        wndAllController.setIgnoreNonInteractiveChanges(false);
        wndAllController.setEnabled(true);
        JCheckBox wndAllCheckbox = new JCheckBox("wA");
        wndAllCheckbox.setToolTipText("Window All Images");
        toolbar.add(wndAllCheckbox);
        Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
                wndAllController, BeanProperty.create("enabled"),
                wndAllCheckbox, BeanProperty.create("selected")).bind();

        toolbar.add(new AbstractAction("zRST") {

            {
                putValue(Action.SHORT_DESCRIPTION, "Reset Zoom/Pan");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                zpAllController.runWithControllerInhibited(new Runnable() {

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
                zpAllController.runWithControllerInhibited(new Runnable() {

                    @Override
                    public void run() {
                        lazyZoomPanInitializationController.reset();
                    }
                });
            }
        });
        zpAllController = new ImageListViewZoomPanApplyToAllController(listView);
        zpAllController.setIgnoreNonInteractiveChanges(false);
        zpAllController.setEnabled(true);
        JCheckBox zpAllCheckbox = new JCheckBox("zA");
        zpAllCheckbox.setToolTipText("Zoom/Pan All Images");
        toolbar.add(zpAllCheckbox);
        Bindings.createAutoBinding(UpdateStrategy.READ_WRITE,
                zpAllController, BeanProperty.create("enabled"),
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

        syncButtonsToolbar = new JToolBar();
        syncButtonsToolbar.setFloatable(false);
        toolbar.add(syncButtonsToolbar);
    }

    public void setParentFrameView(BRFrameView parentFrameView) {
        this.parentFrameView = parentFrameView;
    }

    public ImageListView getListView() {
        return listView;
    }

    public JToolBar getToolbar() {
        return toolbar;
    }

    public JToolBar getSyncButtonsToolbar() {
        return syncButtonsToolbar;
    }

    public void toggleInfo() {
        infoMode = (infoMode + 1) % 3;
        listView.refreshCells();
    }

    public void resetAllWindowing() {
        lazyWindowingToOptimalInitializationController.setEnabled(false);
        lazyWindowingToQCInitializationController.setEnabled(true);
        lazyWindowingToQCInitializationController.reset();
        wndAllController.runWithControllerInhibited(new Runnable() {

            @Override
            public void run() {
                lazyWindowingToQCInitializationController.initializeAllCellsImmediately(false);
            }
        });
    }

    public void addCellMouseListener(MouseListener l) {
        listView.addCellMouseListener(l);
    }

    public boolean isEnlarged() {
        return enlarged;
    }

    public void setEnlarged(boolean enlarged) {
        this.enlarged = enlarged;
        if (!enlarged) {
            if (useJ2DInFrameViews) {
                listView.setScaleMode(new JGridImageListView.MyScaleMode(1, 1));
            } else {
                listView.setScaleMode(new JGLImageListView.MyScaleMode(1, 1));
            }
        }
    }

}
