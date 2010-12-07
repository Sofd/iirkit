package de.sofd.iirkit;

import de.sofd.iirkit.service.IirService;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import org.jdesktop.application.SingleFrameApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class App extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        System.out.println( "Hello World!" );
        //org.mozilla.javascript.tools.shell.Main.main(new String[0]);

        ApplicationContext ctx = new ClassPathXmlApplicationContext("/spring-beans.xml");
        IirService iirSvc = (IirService) ctx.getBean("iirService");
        SecurityContext secCtx = (SecurityContext) ctx.getBean("securityContext");

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
        exit();
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
