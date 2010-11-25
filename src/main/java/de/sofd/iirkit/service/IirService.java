/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.sofd.iirkit.service;

import java.util.List;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author olaf
 */
public interface IirService {

    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    List<User> getAllUsers();

}
