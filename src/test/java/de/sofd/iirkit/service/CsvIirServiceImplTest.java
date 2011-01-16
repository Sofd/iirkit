package de.sofd.iirkit.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

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
    protected void tearDown() throws Exception {
        super.tearDown();
        new File(userFileName).delete();
        new File(caseFileName).delete();
    }

}
