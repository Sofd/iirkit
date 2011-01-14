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
//importClass(Packages.static de.sofd.viskit.util.DicomUtil.PatientBasedMainAxisOrientation);
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

function caseStartingPostFrameInitialization(brContext) {
    print("mssc=" + multiSyncSetController);
}
