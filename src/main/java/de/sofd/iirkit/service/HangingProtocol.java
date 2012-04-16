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

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final HangingProtocol other = (HangingProtocol) obj;
        if (this.seriesGroups != other.seriesGroups && (this.seriesGroups == null || !this.seriesGroups.equals(other.seriesGroups))) {
            return false;
        }
        if ((this.ecrfUrl == null) ? (other.ecrfUrl != null) : !this.ecrfUrl.equals(other.ecrfUrl)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + (this.seriesGroups != null ? this.seriesGroups.hashCode() : 0);
        hash = 23 * hash + (this.ecrfUrl != null ? this.ecrfUrl.hashCode() : 0);
        return hash;
    }

    
}
