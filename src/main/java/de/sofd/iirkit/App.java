package de.sofd.iirkit;

import de.sofd.iirkit.service.Case;
import de.sofd.iirkit.service.IirService;
import de.sofd.iirkit.service.User;
import org.hsqldb.jdbcDriver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App 
{
    public static void main( String[] args ) throws Exception {
        org.hsqldb.jdbcDriver dr = new jdbcDriver();
        System.out.println( "Hello World!" );
        //org.mozilla.javascript.tools.shell.Main.main(new String[0]);

        ApplicationContext ctx = new ClassPathXmlApplicationContext("/spring-beans.xml");
        IirService svc = (IirService) ctx.getBean("iirService");
        for (User user : svc.getAllUsers()) {
            System.out.println("" + user);
        }

        for (Case c : svc.getAllCases()) {
            System.out.println("" + c);
        }

        System.out.println("DONE.");
    }
}
