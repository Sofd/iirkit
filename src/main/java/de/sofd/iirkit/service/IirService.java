/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sofd.iirkit.service;

import java.util.List;

/**
 *
 * @author olaf
 */
public interface IirService {

    List<User> getAllUsers();

    List<Case> getAllCases();

    List<Case> getCasesOf(User user);

    Case getNextCaseOf(User user);

    User authUser(String name, String password);

    User getUser(String name);

    void update(Case c);
}
