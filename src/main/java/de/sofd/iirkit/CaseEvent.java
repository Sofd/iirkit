package de.sofd.iirkit;

import com.google.common.collect.Multimap;
import de.sofd.iirkit.form.FormUtils;
import java.util.EventObject;

/**
 *
 * @author olaf
 */
public class CaseEvent extends EventObject {

    public static enum Type {CASE_OPENED, CASE_SUBMITTED, CASE_CLOSED};

    private final Type type;
    private final Multimap<String, String> requestParams;
    private String formResult;

    public CaseEvent(CaseRunner source, Type type) {
        super(source);
        if (type == Type.CASE_SUBMITTED) {
            throw new IllegalArgumentException("CASE_SUBMITTED event must have requestParams");
        }
        this.type = type;
        this.requestParams = null;
    }

    public CaseEvent(CaseRunner source, Multimap<String, String> requestParams) {
        super(source);
        this.type = Type.CASE_SUBMITTED;
        this.requestParams = requestParams;
    }

    @Override
    public CaseRunner getSource() {
        return (CaseRunner) super.getSource();
    }

    public Type getType() {
        return type;
    }

    public Multimap<String, String> getRequestParams() {
        return requestParams;
    }

    public String getFormResult() {
        if (null == formResult) {
            formResult = FormUtils.paramsToQueryString(requestParams);
        }
        return formResult;
    }

    public boolean isCaseSubmitted() {
        return type == Type.CASE_SUBMITTED;
    }

}
