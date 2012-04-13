package de.sofd.iirkit.form;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.Multimap;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Map.Entry;

/**
 *
 * @author olaf
 */
public class FormUtils {

    public static String paramsToQueryString(Multimap<String, String> requestParams) {
        Collection<String> nameValuePairs = Collections2.transform(requestParams.entries(), new Function<Entry<String,String>, String>() {
            @Override
            public String apply(Entry<String, String> e) {
                try {
                    return e.getKey() + "=" + URLEncoder.encode(e.getValue(), "utf-8");
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException("SHOULD NOT HAPPEN", ex);
                }
            }
        });
        return Joiner.on("&").join(nameValuePairs);
    }
}
