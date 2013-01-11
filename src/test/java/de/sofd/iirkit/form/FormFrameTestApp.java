package de.sofd.iirkit.form;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 *
 * @author olaf
 */
public class FormFrameTestApp {

    private static final Logger logger = Logger.getLogger(FormFrameTestApp.class);

    private final String FORM1_URL = "file:///home/olaf/hieronymusr/iirkit-test/ecrf/312046_11.html";
    private final String FORM2_URL = "file:///home/olaf/hieronymusr/iirkit-test/ecrf/form2.html";
    private final String NOTFOUND_URL = "file:///home/olaf/hieronymusr/iirkit-test/ecrf/foobar_idontexist.html";

    private final FormFrame formFrame;

    public FormFrameTestApp(Display display, String[] args) {
        formFrame = new FormFrame(display);
        formFrame.addFormListener(new FormListener() {
            @Override
            public void formOpened(FormEvent event) {
                logger.info("Event received: " + event);
            }
            @Override
            public void formDeleted(FormEvent event) {
                logger.info("Event received: " + event);
            }
            @Override
            public void formShown(FormEvent event) {
                logger.info("Event received: " + event);
            }
            @Override
            public void formHidden(FormEvent event) {
                logger.info("Event received: " + event);
            }
            @Override
            public void formSubmitted(FormEvent event) {
                logger.info("Event received: " + event);
                logger.info(" result params:");
                for (Entry<String, String> entry: event.getFormResultMap().entries()) {
                    logger.info("   " + entry.getKey() + " = " + entry.getValue());
                }
            }
        });
        formFrame.show();

        Shell controllerFrame = new Shell(display);
        controllerFrame.setText("control");
        ToolBar toolbar = new ToolBar(controllerFrame, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
        addToolItem(toolbar, "showForm");
        addToolItem(toolbar, "hideForm");
        addToolItem(toolbar, "form1");
        addToolItem(toolbar, "form2");
        addToolItem(toolbar, "form_404");
        addToolItem(toolbar, "fill1");
        addToolItem(toolbar, "fill2");
        addToolItem(toolbar, "formfill1");
        addToolItem(toolbar, "formfill2");
        addToolItem(toolbar, "bugtest");
        addToolItem(toolbar, "clear");
        addToolItem(toolbar, "exit");
        addToolItem(toolbar, "test1");
        addToolItem(toolbar, "loadURI");
        addToolItem(toolbar, "loadJquery");
        addToolItem(toolbar, "loadFormutils");
        toolbar.pack();
        controllerFrame.open();
        controllerFrame.pack();
    }
    
    private void addToolItem(ToolBar toolbar, final String actionMethodName) {
        ToolItem itemPush = new ToolItem(toolbar, SWT.PUSH);
        itemPush.setText(actionMethodName);
        itemPush.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event) {
                try {
                    Method m = FormFrameTestApp.this.getClass().getDeclaredMethod(actionMethodName);
                    m.setAccessible(true);
                    m.invoke(FormFrameTestApp.this);
                } catch (Exception e) {
                    logger.error("error running method " + actionMethodName, e);
                }
            }
        });
    }

    private void showForm() {
        formFrame.show();
    }

    private void hideForm() {
        formFrame.hide();
    }

    private void form1() {
        formFrame.setUrl(FORM1_URL);
        logger.debug("form1 loaded");
    }

    private void form2() {
        formFrame.setUrl(FORM2_URL);
        logger.debug("form2 loaded");
    }
    
    private void form_404() {
        formFrame.setUrl(NOTFOUND_URL);
        logger.debug("not-found URL set");
    }

    private void fill1() {
        String paramString = "COMP01_Seq4_COMPISC5=1&foo=bar&COMP01_Seq2_COMPISC5=1&sex=male&weight=45&TRIG01_Seq1_TRIGPERF=1&IMAGE01_Seq1_IMAGAV=&examinationTime=1.2.34&ailments=cancer&ailments=syphillis&headeareyenosethroat=normal&height=123&respiratory=normal&ethnicGroup=black&COMP01_Seq1_COMPISC5=2&age=910&ok=OK&COMP01_Seq3_COMPISC5=2&IMAGE01_Seq1_FILMNO=&cardiovascular=abnormal&gastrointestinal=abnormal&birthData=5.6.78";
        formFrame.setFormContents(paramString);
    }

    private void fill2() {
        Multimap<String, String> params = ArrayListMultimap.create();
        params.put("name", "Hans Meier");
        params.put("street", "Blahweg 5");
        params.put("zip", "10178");
        params.put("city", "Berlin");
        formFrame.setFormContents(params);
    }

    private void formfill1() {
        form1();
        fill1();
    }

    private void formfill2() {
        form2();
        fill2();
    }

    private void clear() {
        Multimap<String, String> params = ArrayListMultimap.create();
        formFrame.setFormContents(params);
    }

    private void bugtest() {
        //s.a. doc/todo.txt
        //
        formFrame.close();
        formFrame.show();
        form1();
        //fill1 (interactively) will err after this (see doc/todo.txt)
        // can also be reproduced interactively: start app, close form, show, form1, fill1
    }
    
    private void test1() {
        //formFrame.runJavascriptInForm("setHeadline('HELLO!!')");
        formFrame.runJavascriptInForm("formutilheadline('foo')");
    }

    private void loadURI() {
        try {
            formFrame.runJavascriptStreamInForm(FormFrame.class.getResourceAsStream("URI.min.js"));
            logger.debug("URI.min.js loaded (manually from test app)");
        } catch (Exception e) {
            logger.error("URI.min.js load error", e);
        }
    }

    private void loadJquery() {
        try {
            formFrame.runJavascriptStreamInForm(FormFrame.class.getResourceAsStream("jquery-1.7.2.min.js"));
            logger.debug("jquery-1.7.2.min.js loaded (manually from test app)");
        } catch (Exception e) {
            logger.error("jquery-1.7.2.min.js load error", e);
        }
    }

    private void loadFormutils() {
        try {
            formFrame.runJavascriptStreamInForm(FormFrame.class.getResourceAsStream("formutils.js"));
            logger.debug("formutils.js loaded (manually from test app)");
        } catch (Exception e) {
            logger.error("formutils.js load error", e);
        }
    }

    private void exit() {
        exit = true;
    }
    
    private static boolean exit = false;

    public static void main(final String[] args) throws Exception {
        Display display = new Display();
        new FormFrameTestApp(display, args);
        while (!exit) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
        display.dispose();
        System.err.println("SWT thread finished.");
   }

}
