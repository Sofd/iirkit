package de.sofd.iirkit.form;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Collection;
import static java.util.Map.Entry;

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
            Collection<String> nameValuePairs = Collections2.transform(requestParams.entries(), new Function<Entry<String,String>, String>() {
                @Override
                public String apply(Entry<String, String> e) {
                    return e.getKey() + "=" + e.getValue();
                }
            });
            formResult = Joiner.on("&").join(nameValuePairs);
        }
        return formResult;
    }

}
