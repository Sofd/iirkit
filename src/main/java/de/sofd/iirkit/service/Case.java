package de.sofd.iirkit.service;

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

    public Case(int number, String hangingProtocol, String result) {
        this.number = number;
        this.hangingProtocol = hangingProtocol;
        this.result = result;
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
    }

    @Override
    public String toString() {
        return "[Case: nr=" + getNumber() + ", hp=" + getHangingProtocol() + ", res=" + getResult() + "]";
    }

}
