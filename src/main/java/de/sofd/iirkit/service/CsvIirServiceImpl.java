package de.sofd.iirkit.service;

import de.sofd.util.BiHashMap;
import de.sofd.util.BiMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;

/**
 *
 * @author olaf
 */
public class CsvIirServiceImpl implements IirService {

    protected static Logger logger = Logger.getLogger(CsvIirServiceImpl.class);

    //strategy: read the users+cases CSV files once at creation time, keep the whole users+cases database
    //in memory all the time (in the following two member variables),
    //hand out copies of User/Case objects only, atomically rewrite the whole CSV file on every change to the database.
    //some O(n) (unoptimized) methods; implementation won't scale to really large numbers of users or cases
    //implementation is not thread-safe.
    private final Map<String, User> usersByName = new LinkedHashMap<String, User>();
    private final List<Case> cases = new ArrayList<Case>();

    private final File userCsvFile, caseCsvFile;

    private final BiMap<String, Integer> caseCsvHeaderColumnNrsByName;

    private static final String[] REQUIRED_CASE_HDRS = new String[]{
        Case.ATTR_NAME_USER, Case.ATTR_NAME_CASE, Case.ATTR_NAME_HP, Case.ATTR_NAME_RESULT
    };

    public CsvIirServiceImpl(String userCsvFileName, String caseCsvFileName) {
        this(new File(userCsvFileName), new File(caseCsvFileName));
    }

    public CsvIirServiceImpl(File userCsvFile, File caseCsvFile) {
        this.userCsvFile = userCsvFile;
        this.caseCsvFile = caseCsvFile;
        //for error reportage
        File file = null;
        int lineNo = -1;
        try {
            {
                file = userCsvFile;
                CSVParser p = new CSVParser(new InputStreamReader(new FileInputStream(userCsvFile), "utf-8"));
                lineNo = 1;
                String[] line = p.getLine(); // skip headers
                while (null != (line = p.getLine())) {
                    ++lineNo;
                    if (line.length < 3) {
                        throw new IllegalArgumentException("Illegal line: " + lineToString(line));
                    }
                    User u = new User(line[0], line[1], line[2].split(";"));
                    usersByName.put(u.getName(), u);
                }
            }

            {
                lineNo = -1;
                file = caseCsvFile;
                CSVParser p = new CSVParser(new InputStreamReader(new FileInputStream(caseCsvFile), "utf-8"));

                lineNo = 1;

                String[] line = p.getLine();  //header row
                caseCsvHeaderColumnNrsByName = new BiHashMap<String, Integer>();
                for (int i = 0; i < line.length; i++) {
                    caseCsvHeaderColumnNrsByName.put(line[i], i);
                }
                for (String reqdHdr: REQUIRED_CASE_HDRS) {
                    if (! caseCsvHeaderColumnNrsByName.containsKey(reqdHdr)) {
                        throw new IllegalArgumentException("Required header not found in " + caseCsvFile.getName() + ": " + reqdHdr);
                    }
                }

                Map<String, Integer> lastCaseNrByUserName = new HashMap<String, Integer>(); //for checking for of caseNr continuity
                while (null != (line = p.getLine())) {
                    ++lineNo;

                    Map<String,String> colVals = new HashMap<String, String>();
                    for (String hdrName : caseCsvHeaderColumnNrsByName.keySet()) {
                        int colNr = caseCsvHeaderColumnNrsByName.get(hdrName);
                        String colValue = (line.length > colNr ? line[colNr] : null);
                        colVals.put(hdrName, colValue);
                    }

                    String userName = colVals.get(Case.ATTR_NAME_USER);
                    User user = usersByName.get(userName);
                    if (null == user) {
                        throw new IllegalArgumentException("unknown user: " + userName);
                    }
                    int caseNr = Integer.parseInt(colVals.get(Case.ATTR_NAME_CASE));
                    Integer lastCaseNr = lastCaseNrByUserName.get(userName);
                    if (null == lastCaseNr) {
                        lastCaseNr = 0;
                    }
                    if (caseNr != lastCaseNr + 1) {
                        throw new IllegalArgumentException("Non-continuous case number for user " + userName + ": found: " + caseNr + ", expected: " + (lastCaseNr + 1));
                    }
                    lastCaseNrByUserName.put(userName, caseNr);
                    String res = colVals.get(Case.ATTR_NAME_RESULT);
                    if ("".equals(res)) {
                        res = null;
                    }
                    Case c = new Case(caseNr, colVals.get(Case.ATTR_NAME_HP), res, colVals);
                    c.setUser(user);
                    cases.add(c);
                }
            }
        } catch (Exception e) {
            if (file != null) {
                throw new RuntimeException("Error reading CSV files (" + file + (lineNo==-1? "" : (":"+lineNo)) + "): " + e.getLocalizedMessage(), e);
            } else {
                throw new RuntimeException("Error reading CSV files: " + e.getLocalizedMessage(), e);
            }
        }
    }

    private static String lineToString(String[] line) {
        if (null == line) {
            return "[null]";
        }
        StringBuilder sb = new StringBuilder();
        for (String cell : line) {
            sb.append(cell).append(",");
        }
        return sb.toString();
    }

    private static User copy(User u) {
        if (null == u) {
            return null;
        } else {
            return new User(u.getName(), u.getPassword(), u.getRoles());
        }
    }

    private static Case copy(Case c) {
        if (null == c) {
            return null;
        } else {
            Case result = new Case(c.getNumber(), c.getHangingProtocol(), c.getResult(), c.getAllAttributes());
            result.setUser(copy(c.getUser()));
            return result;
        }
    }

    @Override
    public List<User> getAllUsers() {
        List<User> result = new ArrayList<User>();
        for (User u : usersByName.values()) {
            result.add(copy(u));
        }
        return result;
    }

    @Override
    public User getUser(String name) {
        return copy(usersByName.get(name));
    }

    @Override
    public User authUser(String name, String password) {
        User u = getUser(name);
        if (u == null) {
            throw new RuntimeException("unknown user " + name);
        }
        if (!(password.equals(u.getPassword()))) {
            throw new RuntimeException("wrong password for user " + name);
        }
        return u;
    }

    @Override
    public List<Case> getAllCases() {
        List<Case> result = new ArrayList<Case>();
        for (Case c : cases) {
            result.add(copy(c));
        }
        return result;
    }

    @Override
    public List<Case> getCasesOf(User user) {
        List<Case> result = new ArrayList<Case>();
        for (Case c : cases) {
            if (c.getUser().equals(user)) {
                result.add(copy(c));
            }
        }
        return result;
    }

    @Override
    public int getNumberOfCasesOf(User user) {
        int result = 0;
        for (Case c : cases) {
            if (c.getUser().equals(user)) {
                ++result;
            }
        }
        return result;
    }

    @Override
    public int getNumberOfDoneCasesOf(User user) {
        int result = 0;
        for (Case c : cases) {
            if (c.getUser().equals(user) && c.getResult() != null) {
                ++result;
            }
        }
        return result;
    }

    @Override
    public Case getNextCaseOf(User user) {
        for (Case c : cases) {
            if (c.getUser().equals(user) && c.getResult() == null) {
                return copy(c);
            }
        }
        return null;
    }

    @Override
    public int update(Case c) {
        for (Case c2 : cases) {
            if (c2.getUser().equals(c.getUser()) && c2.getNumber() == c.getNumber()) {
                c2.setHangingProtocol(c.getHangingProtocol());
                c2.setResult(c.getResult());
                try {
                    persistDatabase();
                } catch (IOException ex) {
                    throw new RuntimeException("failed to persist data to CSV file: " + ex.getLocalizedMessage(), ex);
                }
                //TODO: c2 should be updated only here to ensure consistency (i.e. no change to in-memory DB if persisting failed)
                return 1;
            }
        }
        return 0;
    }

    private void persistDatabase() throws IOException {
        File caseTmpFile = new File(caseCsvFile.getParentFile(), caseCsvFile.getName() + ".new");
        caseTmpFile.createNewFile();
        Writer w = new OutputStreamWriter(new FileOutputStream(caseTmpFile), "utf-8");
        try {
            CSVPrinter printer = new CSVPrinter(w);
            int nColumns = caseCsvHeaderColumnNrsByName.size();
            for (int col = 0; col < nColumns; col++) {
                printer.print(caseCsvHeaderColumnNrsByName.reverseGet(col));
            }
            printer.println();
            for (Case c : cases) {
                for (int colNr = 0; colNr < nColumns; colNr++) {
                    String colName = caseCsvHeaderColumnNrsByName.reverseGet(colNr);
                    printer.print(nonNull(c.getAllAttributes().get(colName)));
                }
                printer.println();
            }
        } finally {
            w.close();
        }
        if (!caseTmpFile.renameTo(caseCsvFile)) {
            throw new IOException("failed to rename new to old file after writing case CSV list");
        }
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

}
