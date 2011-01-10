package de.sofd.iirkit.form;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import static java.util.Map.Entry;

/**
 *
 * @author olaf
 */
public class FormDoneEvent {

    private String submitUrl;
    private Map<String,String> requestParams;
    private String formResult;

    public FormDoneEvent() {
    }

    public FormDoneEvent(String submitUrl, Map<String,String> requestParams) {
        this.submitUrl = submitUrl;
        this.requestParams = Collections.unmodifiableMap(requestParams);
    }

    public boolean isFormSubmitted() {
        return null != requestParams;
    }

    public String getSubmitUrl() {
        return submitUrl;
    }

    public Map<String, String> getRequestParams() {
        return requestParams;
    }

    public String getFormResult() {
        if (null == formResult) {
            StringBuilder sb = new StringBuilder();
            for (Iterator<Entry<String,String>> entryIt = requestParams.entrySet().iterator(); entryIt.hasNext();) {
                Entry<String,String> e = entryIt.next();
                sb.append("").append(e.getKey()).append("=").append(e.getValue());
                if (entryIt.hasNext()) {
                    sb.append("&");
                }
            }
            formResult = sb.toString();
        }
        return formResult;
    }

}
