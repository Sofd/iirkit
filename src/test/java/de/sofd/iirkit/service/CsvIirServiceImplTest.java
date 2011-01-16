package de.sofd.iirkit.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import org.apache.commons.csv.CSVParser;
import org.junit.Assert;

/**
 *
 * @author olaf
 */
public class CsvIirServiceImplTest extends IirServiceImplTest {

    private static final String userFileName = "/tmp/iirsvctest/user.csv";
    private static final String caseFileName = "/tmp/iirsvctest/case.csv";

    public CsvIirServiceImplTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        File userFile = new File(userFileName);
        userFile.getParentFile().mkdirs();
        userFile.delete();
        File caseFile = new File(caseFileName);
        caseFile.getParentFile().mkdirs();
        caseFile.delete();

        String userContents = "name,password,roles\n"
                + "olaf,olafpw,admin\n"
                + "hans,hanspw,user;admin\n";
        userFile.createNewFile();
        Writer w = new OutputStreamWriter(new FileOutputStream(userFile), "utf-8");
        try {
            w.write(userContents);
        } finally {
            w.close();
        }
        
        String caseContents = "user,case,hangingProtocol,result\n"
                + "olaf,1,c1hp,c1res\n"
                + "olaf,2,c2hp,c2res\n"
                + "olaf,3,c3hp,\n"
                + "olaf,4,c4hp,\n"
                + "olaf,5,c5hp,\n"
                + "olaf,6,c6hp,\n"
                + "hans,1,hc1hp,hc1res\n"
                + "hans,2,hc2hp,hc2res\n"
                + "hans,3,hc3hp,hc3res\n"
                + "hans,4,hc4hp,\n"
                + "hans,5,hc5hp,\n";
        caseFile.createNewFile();
        w = new OutputStreamWriter(new FileOutputStream(caseFile), "utf-8");
        try {
            w.write(caseContents);
        } finally {
            w.close();
        }

        svc = new CsvIirServiceImpl(userFileName, caseFileName);
    }

    @Override
    public void testUpdate() {
        super.testUpdate();
        //white-box test for correct CSV contents after testUpdate run
        try {
            File caseFile = new File(caseFileName);
            CSVParser parser = new CSVParser(new InputStreamReader(new FileInputStream(caseFile), "utf-8"));
            Assert.assertArrayEquals(new String[]{"user","case","hangingProtocol","result"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"olaf","1","c1hp","c1res"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"olaf","2","c2hp","c2res"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"olaf","3","c3hp","res3"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"olaf","4","c4hp","res4"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"olaf","5","c5hp","res5"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"olaf","6","c6hp","res6"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"hans","1","hc1hp","hc1res"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"hans","2","hc2hp","hc2res"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"hans","3","hc3hp","hc3res"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"hans","4","hc4hp","hres4"}, parser.getLine());
            Assert.assertArrayEquals(new String[]{"hans","5","hc5hp",""}, parser.getLine());
            assertNull(parser.getLine());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        new File(userFileName).delete();
        new File(caseFileName).delete();
    }

}
