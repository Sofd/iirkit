package de.sofd.iirkit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author olaf
 */
public class Case {

    protected int number;
    protected User user;
    protected List<String> seriesGroupUrls;
    protected String ecrfUrl;
    protected String result;
    protected HangingProtocol hp;
    protected Map<String, String> allAttrs;

    public static final String ATTR_NAME_USER = "user";
    public static final String ATTR_NAME_CASE = "case";
    public static final String ATTR_NAME_SERIES_GROUP = "seriesGroup";
    public static final String ATTR_NAME_ECRF = "ecrf";
    public static final String ATTR_NAME_RESULT = "result";

    public Case(int number, List<String> seriesGroupUrls, String ecrfUrl, String result) {
        this(number, seriesGroupUrls, ecrfUrl, result, null);
    }

    //TODO: (number, hangingProtocol, result) and attrs are interdependent; passing
    // non-matching values leads to a invalid Case instance. Get rid of all member variables
    // except allAttrs
    public Case(int number, List<String> seriesGroupUrls, String ecrfUrl, String result, Map<String, String> attrs) {
        this.number = number;
        this.seriesGroupUrls = new ArrayList<String>(seriesGroupUrls);
        this.ecrfUrl = ecrfUrl;
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

    public List<String> getSeriesGroupUrls() {
        return Collections.unmodifiableList(seriesGroupUrls);
    }

    public void setSeriesGroupUrls(List<String> seriesGroupUrls) {
        this.seriesGroupUrls = seriesGroupUrls;
        int i = 0;
        for (String sgUrl : seriesGroupUrls) {
            allAttrs.put(ATTR_NAME_SERIES_GROUP + (i+1), sgUrl);
            i++;
        }
        while (null != allAttrs.get(ATTR_NAME_SERIES_GROUP + (i+1))) {
            allAttrs.remove(ATTR_NAME_SERIES_GROUP + (i+1));
            i++;
        }
    }

    public String getEcrfUrl() {
        return ecrfUrl;
    }

    public void setEcrfUrl(String ecrfUrl) {
        this.ecrfUrl = ecrfUrl;
        allAttrs.put(ATTR_NAME_ECRF, ecrfUrl);
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
    public HangingProtocol getHangingProtocol() {
        if (null == hp) {
            hp = new HangingProtocol(getSeriesGroupUrls(), getEcrfUrl());
        }
        return hp;
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
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Case other = (Case) obj;
        if (this.number != other.number) {
            return false;
        }
        if (this.user != other.user && (this.user == null || !this.user.equals(other.user))) {
            return false;
        }
        if (this.seriesGroupUrls != other.seriesGroupUrls && (this.seriesGroupUrls == null || !this.seriesGroupUrls.equals(other.seriesGroupUrls))) {
            return false;
        }
        if ((this.ecrfUrl == null) ? (other.ecrfUrl != null) : !this.ecrfUrl.equals(other.ecrfUrl)) {
            return false;
        }
        if ((this.result == null) ? (other.result != null) : !this.result.equals(other.result)) {
            return false;
        }
        if (this.hp != other.hp && (this.hp == null || !this.hp.equals(other.hp))) {
            return false;
        }
        if (this.allAttrs != other.allAttrs && (this.allAttrs == null || !this.allAttrs.equals(other.allAttrs))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 83 * hash + this.number;
        hash = 83 * hash + (this.user != null ? this.user.hashCode() : 0);
        hash = 83 * hash + (this.seriesGroupUrls != null ? this.seriesGroupUrls.hashCode() : 0);
        hash = 83 * hash + (this.ecrfUrl != null ? this.ecrfUrl.hashCode() : 0);
        hash = 83 * hash + (this.result != null ? this.result.hashCode() : 0);
        hash = 83 * hash + (this.hp != null ? this.hp.hashCode() : 0);
        hash = 83 * hash + (this.allAttrs != null ? this.allAttrs.hashCode() : 0);
        return hash;
    }

   
    @Override
    public String toString() {
        return "[Case: nr=" + getNumber() + ", hp=" + getHangingProtocol() + ", res=" + getResult() + "]";
    }

}
