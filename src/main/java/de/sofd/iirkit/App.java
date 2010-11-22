package de.sofd.iirkit;

import org.hsqldb.jdbcDriver;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception {
        org.hsqldb.jdbcDriver dr = new jdbcDriver();
        System.out.println( "Hello World!" );
        org.mozilla.javascript.tools.shell.Main.main(new String[0]);

        System.out.println("DONE.");
    }
}
