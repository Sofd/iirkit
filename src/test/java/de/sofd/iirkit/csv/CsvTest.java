package de.sofd.iirkit.csv;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;

/**
 *
 * @author olaf
 */
public class CsvTest {

    public static void main(String[] args) throws Exception {
        CSVParser parser = new CSVParser(new InputStreamReader(new FileInputStream("/home/olaf/doc/mydocs/csv/tests.csv"), "utf-8"));
        String[][] data = parser.getAllValues();
        for (String[] line: data) {
            for (String cell: line) {
                System.out.print(">>" + cell + "<< ");
            }
            System.out.println();
        }

        System.out.println("\n\n\n");

        CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(System.out, "utf-8"));
        //CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(new FileOutputStream("/home/olaf/doc/mydocs/csv/testsout.csv"), "utf-8"));
        printer.println(data);
        System.out.println("\n\n\n");
        printer.println(new String[]{"foo","bar","baz"});
        printer.println(new String[]{"42","23","88"});
        printer.print("hello");
        printer.print("world");
        printer.print("42");
        printer.println();
        printer.print("tralala");
        printer.print("23");
        printer.print(""/*null triggers NPE*/);
        printer.println();
    }
}
