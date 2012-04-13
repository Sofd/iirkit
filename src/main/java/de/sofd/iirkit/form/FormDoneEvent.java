package de.sofd.iirkit.form;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 *
 * @author olaf
 */
public class FormDoneEvent {

    private String submitUrl;
    private Multimap<String, String> requestParams;
    private String formResult;

    public FormDoneEvent() {
    }

    public FormDoneEvent(String submitUrl, Multimap<String,String> requestParams) {
        this.submitUrl = submitUrl;
        this.requestParams = Multimaps.unmodifiableMultimap(requestParams);
    }

    public boolean isFormSubmitted() {
        return null != requestParams;
    }

    public String getSubmitUrl() {
        return submitUrl;
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

}
