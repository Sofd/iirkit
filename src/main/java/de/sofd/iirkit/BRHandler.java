package de.sofd.iirkit;

import de.sofd.iirkit.form.FormFrame;
import de.sofd.lang.Function2;
import de.sofd.lang.Runnable2;
import de.sofd.viskit.controllers.ImageListViewInitialWindowingController;
import de.sofd.viskit.controllers.ImageListViewInitialZoomPanController;
import de.sofd.viskit.controllers.ImageListViewWindowingApplyToAllController;
import de.sofd.viskit.controllers.ImageListViewZoomPanApplyToAllController;
import de.sofd.viskit.controllers.MultiILVSyncSetController;
import de.sofd.viskit.controllers.cellpaint.ImageListViewPrintTextToCellsController;
import de.sofd.viskit.ui.imagelist.JImageListView;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import org.apache.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

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
    private AppConfig appConfig;

    public AppConfig getAppConfig() {
        return appConfig;
    }

    public void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * For use from Javascript.
     * 
     * @param s
     */
    public static void print(String s) {
        System.out.println(s);
    }

    private Reader getBrHandlerJsReader() throws IOException {
        File jsFile = new File(appConfig.getBaseDir(), "brhandler.js");
        if (jsFile.exists()) {
            return new InputStreamReader(new FileInputStream(jsFile), "utf-8");
        } else {
            //create the file by copying the one from our classpath
            Writer out = new OutputStreamWriter(new FileOutputStream(jsFile), "utf-8");
            try {
                Reader in = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("de/sofd/iirkit/resources/scripts/brhandler.js"), "utf-8");
                char[] buf = new char[5000];
                int n = in.read(buf);
                while (n >= 0) {
                    out.write(buf, 0, n);
                    n = in.read(buf);
                }
            } finally {
                out.close();
            }
            return new InputStreamReader(new FileInputStream(jsFile), "utf-8");
        }
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
                ScriptableObject jsScopeTmp = new ImporterTopLevel(cx); //cx.initStandardObjects();
                if (!isJsInitialized) {
                    try {
                        jsScopeTmp.defineFunctionProperties(new String[]{"print"}, BRHandler.class, ScriptableObject.DONTENUM);
                        Reader r = getBrHandlerJsReader();
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
                    logger.debug("function not defined in script: " + name);
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

    /**
     * Called once per
     * frame and case (and thus potentially multiple times per frame, as frames
     * may be reused between cases). The method should place and
     * intialize the frame (not the view panels/listViews inside it)
     */
    void initializeFrame(BRFrameView frame, int frameNo, BRContext brContext) {
        callJsFunction("initializeFrame", frame, frameNo, brContext);
    }

    Rectangle getFormFrameBounds(BRContext brContext) {
        Wrapper wrapper = (Wrapper) callJsFunction("getFormFrameBounds", brContext);
        return wrapper == null ? null : (Rectangle) wrapper.unwrap();
    }

    void initializeFormFrame(FormFrame formFrame, BRContext brContext) {
        callJsFunction("initializeFormFrame", formFrame, brContext);
    }

    void initializeViewPanel(BRViewPanel panel, ListModel/*or ModelFactory+key?*/ seriesModel, BRContext brContext) {
        callJsFunction("initializeViewPanel", panel, seriesModel, brContext);
    }

    /**
     * Called when a view panel is (possibly temporarily) no longer used to display a series.
     *
     * @param panel
     * @param brContext
     */
    void resetViewPanel(BRViewPanel panel, BRContext brContext) {
        callJsFunction("resetViewPanel", panel, brContext);
    }

    void caseStartingPostFrameInitialization(BRContext brContext) {
        callJsFunction("caseStartingPostFrameInitialization", brContext);
    }


    void frameDisposing(BRFrameView frame, int frameNo, BRContext brContext) {
        callJsFunction("frameDisposing", frame, frameNo, brContext);
    }

    /**
     * OWC calls this when it recognizes that the user clicked OK on
     * the form. formResult already written to
     * brContext.currentCase.result?
     */
    void caseFinished(BRContext brContext) {
        callJsFunction("caseFinished", brContext);
    }

}
