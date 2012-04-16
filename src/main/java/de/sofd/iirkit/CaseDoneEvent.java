package de.sofd.iirkit;

import com.google.common.collect.Multimap;
import de.sofd.iirkit.form.FormUtils;
import java.util.EventObject;

/**
 *
 * @author olaf
 */
public class CaseDoneEvent extends EventObject {

    private final Multimap<String, String> requestParams;
    private String formResult;

    public CaseDoneEvent(CaseRunner source, Multimap<String, String> requestParams) {
        super(source);
        this.requestParams = requestParams;
    }

    @Override
    public CaseRunner getSource() {
        return (CaseRunner) super.getSource();
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

    public boolean isCaseFinished() {
        return requestParams != null;
    }

}
