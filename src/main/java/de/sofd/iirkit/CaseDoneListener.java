package de.sofd.iirkit;

import java.util.EventListener;

/**
 *
 * @author olaf
 */
public interface CaseDoneListener extends EventListener {

    void caseFinished(CaseDoneEvent e);

    void caseCancelled(CaseDoneEvent e);
}
