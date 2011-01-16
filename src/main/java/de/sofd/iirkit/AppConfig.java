package de.sofd.iirkit;

import java.io.File;

/**
 *
 * @author olaf
 */
public class AppConfig {

    protected String baseDirName;

    public AppConfig() {
        baseDirName = ".";
    }

    /**
     * Get the value of baseDirName
     *
     * @return the value of baseDirName
     */
    public String getBaseDirName() {
        return baseDirName;
    }

    public File getBaseDir() {
        File result = new File(getBaseDirName());
        if (result.isDirectory()) {
            return result;
        } else {
            throw new IllegalStateException("base directory not found: " + getBaseDirName());
        }
    }

    /**
     * Set the value of baseDirName
     *
     * @param baseDirName new value of baseDirName
     */
    public void setBaseDirName(String baseDirName) {
        this.baseDirName = baseDirName;
    }

}
