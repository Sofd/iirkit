package de.sofd.iirkit;

import java.util.EventListener;

/**
 *
 * @author olaf
 */
public interface CaseListener extends EventListener {

    void caseOpened(CaseEvent e);

    void caseSubmitted(CaseEvent e);

    void caseClosed(CaseEvent e);
}
