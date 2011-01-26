package de.sofd.iirkit.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author olaf
 */
public class HangingProtocol {
    private final List<SeriesGroup> seriesGroups = new ArrayList<SeriesGroup>();
    private String ecrfUrl;

    HangingProtocol(List<String> seriesGroupUrls, String ecrfUrl) {
        for (String sgUrl : seriesGroupUrls) {
            this.seriesGroups.add(new SeriesGroup(sgUrl));
        }
        this.ecrfUrl = ecrfUrl;
    }

    public List<SeriesGroup> getSeriesGroups() {
        return Collections.unmodifiableList(seriesGroups);
    }

    public String getEcrfUrl() {
        return ecrfUrl;
    }

}
