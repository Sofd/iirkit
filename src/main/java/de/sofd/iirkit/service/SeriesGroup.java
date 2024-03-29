package de.sofd.iirkit.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author olaf
 */
public class SeriesGroup {
    private final List<String> seriesUrls = new ArrayList<String>();

    public List<String> getSeriesUrls() {
        return Collections.unmodifiableList(seriesUrls);
    }

    public int getSeriesUrlsCount() {
        return seriesUrls.size();
    }

    public String getSeriesUrl(int i) {
        return seriesUrls.get(i);
    }

    public SeriesGroup(String baseDirName) {
        try {
            File baseDir = new File(baseDirName);
            if (!(baseDir.exists() && baseDir.isDirectory())) {
                throw new IllegalStateException("absent/invalid series group directory: " + baseDirName);
            }
            for (File f : baseDir.listFiles()) {
                if (f.isDirectory()) {
                    seriesUrls.add(f.getCanonicalPath());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("error reading base dir " + baseDirName + ": " + e.getLocalizedMessage(), e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SeriesGroup other = (SeriesGroup) obj;
        if (this.seriesUrls != other.seriesUrls && (this.seriesUrls == null || !this.seriesUrls.equals(other.seriesUrls))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.seriesUrls != null ? this.seriesUrls.hashCode() : 0);
        return hash;
    }

}
