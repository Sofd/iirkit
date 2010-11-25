package de.sofd.iirkit;

import de.sofd.iirkit.service.IirService;
import de.sofd.iirkit.service.HsqlIirServiceImpl;
import de.sofd.iirkit.service.User;
import org.hsqldb.jdbcDriver;

public class App 
{
    public static void main( String[] args ) throws Exception {
        org.hsqldb.jdbcDriver dr = new jdbcDriver();
        System.out.println( "Hello World!" );
        //org.mozilla.javascript.tools.shell.Main.main(new String[0]);

        IirService svc = new HsqlIirServiceImpl();
        for (User user : svc.getAllUsers()) {
            System.out.println("" + user);
        }
        
        System.out.println("DONE.");
    }
}
