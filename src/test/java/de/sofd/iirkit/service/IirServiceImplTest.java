package de.sofd.iirkit.service;

import java.util.List;
import junit.framework.TestCase;

/**
 *
 * @author olaf
 */
public class IirServiceImplTest extends TestCase {

    protected IirService svc;

    public IirServiceImplTest(String name) {
        super(name);
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

    public void testGetAllCases() {
        System.out.println("getAllCases");
        List<Case> all = svc.getAllCases();
        assertEquals(11, all.size());
    }

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

    public void testGetCasesOf() {
        System.out.println("getCasesOf");
        User olaf = svc.getUser("olaf");
        List<Case> cases = svc.getCasesOf(olaf);
        assertEquals(6, cases.size());
        cases.get(0).getEcrfUrl().startsWith("c");
        User hans = svc.getUser("hans");
        cases = svc.getCasesOf(hans);
        assertEquals(5, cases.size());
        cases.get(0).getEcrfUrl().startsWith("hc");
    }

    public void testGetNextCaseOf() {
        System.out.println("getNextCaseOf");
        User olaf = svc.getUser("olaf");
        Case c = svc.getNextCaseOf(olaf);
        assertEquals(3, c.getNumber());
        assertEquals("c3hp", c.getEcrfUrl());
        User hans = svc.getUser("hans");
        c = svc.getNextCaseOf(hans);
        assertEquals(4, c.getNumber());
        assertEquals("hc4hp", c.getEcrfUrl());
    }

    public void testGetUser() {
        System.out.println("getUser");
        User u0 = svc.getUser("olaf");
        assertEquals("olaf", u0.getName());
        assertEquals("olafpw", u0.getPassword());
        assertEquals(1, u0.getRoles().size());
        assertEquals("admin", u0.getRoles().iterator().next());
    }

    public void testNumberOfCases() {
        System.out.println("numberOfCases");
        User olaf = svc.getUser("olaf");
        assertEquals(6, svc.getNumberOfCasesOf(olaf));
        assertEquals(2, svc.getNumberOfDoneCasesOf(olaf));
    }

    public void testUpdate() {
        System.out.println("update");
        User olaf = svc.getUser("olaf");
        User hans = svc.getUser("hans");
        Case c = svc.getNextCaseOf(olaf);
        assertEquals(3, c.getNumber());
        assertEquals("c3hp", c.getEcrfUrl());
        assertNull(c.getResult());
        c.setResult("res3");
        assertEquals("res3", c.getResult());
        svc.update(c);
        c = svc.getNextCaseOf(hans);
        assertEquals(4, c.getNumber());
        assertEquals("hc4hp", c.getEcrfUrl());
        assertNull(c.getResult());
        c.setResult("hres4");
        assertEquals("hres4", c.getResult());
        svc.update(c);
        c = svc.getNextCaseOf(hans);
        assertEquals(5, c.getNumber());
        assertEquals("hc5hp", c.getEcrfUrl());
        c = svc.getNextCaseOf(olaf);
        assertEquals(4, c.getNumber());
        assertEquals("c4hp", c.getEcrfUrl());
        assertNull(c.getResult());
        c.setResult("res4");
        svc.update(c);
        c = svc.getNextCaseOf(olaf);
        assertEquals(5, c.getNumber());
        assertEquals("c5hp", c.getEcrfUrl());
        assertNull(c.getResult());
        c.setResult("res5");
        svc.update(c);
        c = svc.getNextCaseOf(olaf);
        assertEquals(6, c.getNumber());
        assertEquals("c6hp", c.getEcrfUrl());
        assertNull(c.getResult());
        c.setResult("res6");
        svc.update(c);
        c = svc.getNextCaseOf(olaf);
        assertNull(c);
    }

}
