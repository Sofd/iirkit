package de.sofd.iirkit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author olaf
 */
public class HangingProtocol {
    private final List<SeriesGroup> seriesGroups = new ArrayList<SeriesGroup>();
    private String ecrfUrl;
    private final Map<String, String> attributes = new HashMap<String, String>();

    HangingProtocol(String serialized) {
        //TODO: regexs don't really work for this? Use a real lexer, maybe from antlr
        for (String namevalue: serialized.split(";")) {   //TODO: use look-ahead group to not match when "\" precedes
            int eqIdx = namevalue.indexOf("=");
            if (eqIdx == -1) {  //if the group base URL contains "=", use series=<url> (see below)
                seriesGroups.add(new SeriesGroup(namevalue));
            } else {
                String name = namevalue.substring(0, eqIdx);
                String value = namevalue.substring(eqIdx + 1);
                if ("series".equals(name)) {
                    seriesGroups.add(new SeriesGroup(value));
                } else if ("ecrf".equals(name)) {
                    ecrfUrl = value;
                } else {
                    attributes.put(name, value);
                }
            }
        }
    }

    public List<SeriesGroup> getSeriesGroups() {
        return Collections.unmodifiableList(seriesGroups);
    }

    public String getEcrfUrl() {
        return ecrfUrl;
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

}