package de.sofd.iirkit.form;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 *
 * @author olaf
 */
public class FormDoneEvent {

    private final String submitUrl;
    private final Multimap<String, String> requestParams;
    private String formResult;

    public FormDoneEvent() {
        submitUrl = null;
        requestParams = null;
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

    public Multimap<String, String> getFormResultMap() {
        return requestParams;
    }

    public String getFormResult() {
        if (null == formResult) {
            formResult = FormUtils.paramsToQueryString(requestParams);
        }
        return formResult;
    }

}
