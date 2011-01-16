package de.sofd.iirkit;

import de.sofd.iirkit.service.CsvIirServiceImpl;
import de.sofd.iirkit.service.IirService;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.File;
import javax.swing.JFrame;
import org.apache.log4j.Logger;
import org.jdesktop.application.SingleFrameApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class App extends SingleFrameApplication {

    static final Logger logger = Logger.getLogger(App.class);

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        logger.info( "startup" );
        //org.mozilla.javascript.tools.shell.Main.main(new String[0]);

        System.setProperty("sun.awt.exception.handler", AwtExceptionHandler.class.getName());
        //TODO: QT thread exception handler?

        //this spring crap is really overdesigned -- IoC would better be done manually,
        //creating and wiring up all the "beans" from here directly

        ApplicationContext ctx = new ClassPathXmlApplicationContext("/spring-beans.xml");

        AppConfig appConfig = (AppConfig) ctx.getBean("appConfig");
        //TODO: possibly modify appConfig.baseDirName from some command line parameter

        //IirService iirSvc = (IirService) ctx.getBean("iirService");
        IirService iirSvc = new CsvIirServiceImpl(new File(appConfig.getBaseDir(), "user.csv"), new File(appConfig.getBaseDir(), "case.csv"));

        //SecurityContext secCtx = (SecurityContext) ctx.getBean("securityContext");
        SecurityContext secCtx = new SecurityContext();
        secCtx.setIirService(iirSvc);

        BRHandler brHandler = (BRHandler) ctx.getBean("brHandler");

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        int gsIdx = 0;
        if (gs.length >= 3) {
            gsIdx = 1;
        }
        JFrame dummyFrame = new JFrame(gs[gsIdx].getDefaultConfiguration());
        LoginDialog loginDialog = new LoginDialog(secCtx, dummyFrame, true);
        loginDialog.setLocation((int) gs[gsIdx].getDefaultConfiguration().getBounds().getCenterX() - loginDialog.getWidth(), (int) gs[gsIdx].getDefaultConfiguration().getBounds().getCenterY() - loginDialog.getHeight());
        loginDialog.setVisible(true);

        System.out.println("user=" + secCtx.getUser() + ", authority=" + secCtx.getAuthority());

        SessionControlDialog sessionSelectionDialog = new SessionControlDialog(this, iirSvc, brHandler, secCtx, dummyFrame, true);
        sessionSelectionDialog.setLocation((int) gs[gsIdx].getDefaultConfiguration().getBounds().getCenterX() - loginDialog.getWidth(), (int) gs[gsIdx].getDefaultConfiguration().getBounds().getCenterY() - loginDialog.getHeight());
        sessionSelectionDialog.setVisible(true);
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of App
     */
    public static App getApplication() {
        return App.getInstance(App.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
        launch(App.class, args);
    }

}
