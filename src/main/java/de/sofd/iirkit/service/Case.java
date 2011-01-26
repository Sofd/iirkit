package de.sofd.iirkit.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author olaf
 */
public class Case {

    protected int number;
    protected User user;
    protected String hangingProtocol;
    protected HangingProtocol hpObject;
    protected String result;
    protected Map<String, String> allAttrs;

    public static final String ATTR_NAME_USER   = "user";
    public static final String ATTR_NAME_CASE   = "case";
    public static final String ATTR_NAME_HP     = "hangingProtocol";
    public static final String ATTR_NAME_RESULT = "result";

    public Case(int number, String hangingProtocol, String result) {
        this(number, hangingProtocol, result, null);
    }

    //TODO: (number, hangingProtocol, result) and attrs are interdependent; passing
    // non-matching values leads to a invalid Case instance. Get rid of all member variables
    // except allAttrs
    public Case(int number, String hangingProtocol, String result, Map<String, String> attrs) {
        this.number = number;
        this.hangingProtocol = hangingProtocol;
        this.result = result;
        if (attrs == null) {
            this.allAttrs = new HashMap<String, String>();
        } else {
            this.allAttrs = new HashMap<String, String>(attrs);
        }
    }

    /**
     * Get the value of result
     *
     * @return the value of result
     */
    public String getResult() {
        return result;
    }

    /**
     * Set the value of result
     *
     * @param result new value of result
     */
    public void setResult(String result) {
        this.result = result;
        allAttrs.put(ATTR_NAME_RESULT, result);
    }

    /**
     * Get the value of hangingProtocol
     *
     * @return the value of hangingProtocol
     */
    public String getHangingProtocol() {
        return hangingProtocol;
    }

    /**
     * Set the value of hangingProtocol
     *
     * @param hangingProtocol new value of hangingProtocol
     */
    public void setHangingProtocol(String hangingProtocol) {
        this.hangingProtocol = hangingProtocol;
        this.hpObject = null;
        allAttrs.put(ATTR_NAME_HP, hangingProtocol);
    }

    /**
     * The hanging protocol in parsed-out representation.
     *
     * Name chosen for backward compatibility; should be named
     * getHangingProptocol eventually. The string representation
     * (database field) shouldn't be exposed in the public API, really
     *
     * @return
     */
    public HangingProtocol getHangingProtocolObject() {
        if (null == hpObject) {
            hpObject = new HangingProtocol(getHangingProtocol());
        }
        return hpObject;
    }

    /**
     * Get the value of number
     *
     * @return the value of number
     */
    public int getNumber() {
        return number;
    }

    /**
     * Set the value of number
     *
     * @param number new value of number
     */
    public void setNumber(int number) {
        this.number = number;
        allAttrs.put(ATTR_NAME_CASE, "" + number);
    }

    /**
     * Get the value of user
     *
     * @return the value of user
     */
    public User getUser() {
        return user;
    }

    /**
     * Set the value of user
     *
     * @param user new value of user
     */
    public void setUser(User user) {
        this.user = user;
        allAttrs.put(ATTR_NAME_USER, user.getName());
    }

    public Map<String, String> getAllAttributes() {
        return Collections.unmodifiableMap(allAttrs);
    }

    public String getAttribute(String name) {
        return allAttrs.get(name);
    }

    @Override
    public String toString() {
        return "[Case: nr=" + getNumber() + ", hp=" + getHangingProtocol() + ", res=" + getResult() + "]";
    }

}
