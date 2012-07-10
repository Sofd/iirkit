package de.sofd.iirkit.form;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 *
 * @author Olaf Klischat
 */
public class FormEvent {

    public static enum Type {FORM_SUBMITTED, FORM_OPENED, FORM_CLOSED, FORM_SHOWN, FORM_HIDDEN};

    private final Type type;
    private final String submitUrl;
    private final Multimap<String, String> requestParams;
    private String formResult;

    public FormEvent(Type type) {
        if (type == Type.FORM_SUBMITTED) {
            throw new IllegalArgumentException("FORM_SUBMITTED event must have submitUrl / requestParams");
        }
        this.type = type;
        this.submitUrl = null;
        this.requestParams = null;
    }

    public FormEvent(Type type, String submitUrl, Multimap<String,String> requestParams) {
        this.type = type;
        this.submitUrl = submitUrl;
        this.requestParams = Multimaps.unmodifiableMultimap(requestParams);
    }

    public Type getType() {
        return type;
    }

    public String getSubmitUrl() {
        return submitUrl;
    }

    public Multimap<String, String> getFormResultMap() {
        return requestParams;
    }

    public String getFormResult() {
        if (null == formResult) {
            formResult = FormUtils.paramsToQueryString(requestParams);
        }
        return formResult;
    }

    @Override
    public String toString() {
        return "FormEvent[type=" + getType() + (getType() == Type.FORM_SUBMITTED ? ",submitUrl=" + submitUrl + ",formResult=" + getFormResult() : "") + "]";
    }

}
