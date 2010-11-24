package de.sofd.iirkit.service;

/**
 *
 * @author olaf
 */
public class Case {

    protected int number;
    protected User user;
    protected String hangingProtocol;
    protected String result;

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

}
