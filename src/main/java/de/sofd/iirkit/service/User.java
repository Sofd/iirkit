package de.sofd.iirkit.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author olaf
 */
public class User {

    protected String name;
    protected String password;
    protected List<String> roles;

    public User(String name, String password, Collection<String> roles) {
        this.name = name;
        this.password = password;
        this.roles = new ArrayList<String>(roles);
    }

    public User(String name, String password, String[] roles) {
        this.name = name;
        this.password = password;
        this.roles = Arrays.asList(roles);
    }

    /**
     * Get the value of password
     *
     * @return the value of password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the value of password
     *
     * @param password new value of password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get the value of name
     *
     * @return the value of name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the value of name
     *
     * @param name new value of name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the value of roles
     *
     * @return the value of roles
     */
    public List<String> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    /**
     * Set the value of roles
     *
     * @param roles new value of roles
     */
    public void setRoles(Collection<String> roles) {
        this.roles = new ArrayList<String>(roles);
    }

    @Override
    public String toString() {
        return "[User: name=" + getName() + ", roles=" + roles + "]";
    }

}
