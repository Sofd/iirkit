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

    Case getCaseOf(User user, int caseNr);

    int getNumberOfCasesOf(User user);

    int getNumberOfDoneCasesOf(User user);

    User authUser(String name, String password);

    User getUser(String name);

    int update(Case c);
}
