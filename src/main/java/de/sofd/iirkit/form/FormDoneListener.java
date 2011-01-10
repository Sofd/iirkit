package de.sofd.iirkit.form;

/**
 *
 * @author olaf
 */
public interface FormDoneListener {

    void formSubmitted(FormDoneEvent event);

    void formCancelled(FormDoneEvent event);
}
