/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sofd.iirkit.service;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 *
 * @author olaf
 */
public class HsqlIirServiceImplTest extends TestCase {

    IirService svc;
    SimpleJdbcTemplate svcJdbcTempl;

    public HsqlIirServiceImplTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
		ApplicationContext ctx = new ClassPathXmlApplicationContext("/test-spring-beans.xml");
        svcJdbcTempl = (SimpleJdbcTemplate) ctx.getBean("jdbcTemplate");
        svcJdbcTempl.batchUpdate("insert into user(name,password,roles) values(?,?,?)",
                                 Arrays.asList(new Object[]{"olaf","olafpw","admin"},
                                               new Object[]{"hans","hanspw","user,admin"}));
        svcJdbcTempl.batchUpdate("insert into iircase(userName, caseNr, hangingProtocol, result) values(?,?,?,?)",
                                 Arrays.asList(new Object[]{"olaf",1,"c1hp","c1res"},
                                               new Object[]{"olaf",2,"c2hp","c2res"},
                                               new Object[]{"olaf",3,"c3hp",""},
                                               new Object[]{"olaf",4,"c4hp",""},
                                               new Object[]{"olaf",5,"c5hp",""},
                                               new Object[]{"olaf",6,"c6hp",""},
                                               new Object[]{"hans",1,"hc1hp","hc1res"},
                                               new Object[]{"hans",2,"hc2hp","hc2res"},
                                               new Object[]{"hans",3,"hc3hp","hc3res"},
                                               new Object[]{"hans",4,"hc4hp",""},
                                               new Object[]{"hans",5,"hc5hp",""}));
        svc = (IirService) ctx.getBean("iirService");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        svcJdbcTempl.update("drop table iircase");
        svcJdbcTempl.update("drop table user");
    }

    /**
     * Test of getAllUsers method, of class HsqlIirServiceImpl.
     */
    public void testGetAllUsers() {
        System.out.println("getAllUsers");
        List<User> all = svc.getAllUsers();
        assertEquals(2, all.size());
        User u0 = all.get(0);
        User u1 = all.get(1);
        if (u0.getName().equals("hans")) {
            User tmp = u0;
            u0 = u1;
            u1 = tmp;
        }
        assertEquals("olaf", u0.getName());
        assertEquals("olafpw", u0.getPassword());
        assertEquals(1, u0.getRoles().size());
        assertEquals("admin", u0.getRoles().iterator().next());

        assertEquals("hans", u1.getName());
        assertEquals("hanspw", u1.getPassword());
        assertEquals(2, u1.getRoles().size());
        assertEquals("user", u1.getRoles().get(0));
        assertEquals("admin", u1.getRoles().get(1));
    }

    /**
     * Test of getAllCases method, of class HsqlIirServiceImpl.
     */
    public void testGetAllCases() {
        System.out.println("getAllCases");
        List<Case> all = svc.getAllCases();
        assertEquals(11, all.size());
    }

    public void testGetUser() {
        System.out.println("getUser");
        User u0 = svc.getUser("olaf");
        assertEquals("olaf", u0.getName());
        assertEquals("olafpw", u0.getPassword());
        assertEquals(1, u0.getRoles().size());
        assertEquals("admin", u0.getRoles().iterator().next());
    }

    public void testAuthUser() {
        System.out.println("authUser");
        try {
            svc.authUser("foo", "olafpw");
            fail("exception expected");
        } catch (RuntimeException e) {
            assertEquals("unknown user foo", e.getMessage());
        }
        try {
            svc.authUser("olaf", "wrongpw");
            fail("exception expected");
        } catch (RuntimeException e) {
            assertEquals("wrong password for user olaf", e.getMessage());
        }
        User u0 = svc.authUser("olaf", "olafpw");
        assertEquals("olaf", u0.getName());
        assertEquals("olafpw", u0.getPassword());
        assertEquals(1, u0.getRoles().size());
        assertEquals("admin", u0.getRoles().iterator().next());
    }

    /**
     * Test of getCasesOf method, of class HsqlIirServiceImpl.
     */
    public void testGetCasesOf() {
        System.out.println("getCasesOf");
        User olaf = svc.getUser("olaf");
        List<Case> cases = svc.getCasesOf(olaf);
        assertEquals(6, cases.size());
        cases.get(0).getHangingProtocol().startsWith("c");

        User hans = svc.getUser("hans");
        svc.getCasesOf(hans);
        assertEquals(5, cases.size());
        cases.get(0).getHangingProtocol().startsWith("hc");
    }

    /**
     * Test of getNextCaseOf method, of class HsqlIirServiceImpl.
     */
    public void testGetNextCaseOf() {
        System.out.println("getNextCaseOf");
        User olaf = svc.getUser("olaf");
        Case c = svc.getNextCaseOf(olaf);
        assertEquals(3, c.getNumber());
        assertEquals("c3hp", c.getHangingProtocol());

        User hans = svc.getUser("hans");
        c = svc.getNextCaseOf(hans);
        assertEquals(4, c.getNumber());
        assertEquals("hc4hp", c.getHangingProtocol());
    }

    /**
     * Test of update method, of class HsqlIirServiceImpl.
     */
    public void testUpdate() {
        System.out.println("update");
    }

}
