package de.sofd.iirkit.form;

/**
 *
 * @author Olaf Klischat
 */
public interface FormListener {

    void formSubmitted(FormEvent event);

    void formOpened(FormEvent event);

    void formClosed(FormEvent event);

    void formShown(FormEvent event);

    void formHidden(FormEvent event);

}
