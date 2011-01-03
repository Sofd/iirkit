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
                throw new IllegalStateException("invalid base dir: " + baseDirName);
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
}
