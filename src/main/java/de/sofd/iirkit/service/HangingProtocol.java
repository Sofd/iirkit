package de.sofd.iirkit.service;

import java.io.File;
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
        File ecrfFile = new File(ecrfUrl);
        if (!(ecrfFile.exists() && ecrfFile.isFile())) {
            throw new IllegalStateException("absent/invalid eCRF file: " + ecrfUrl);
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
