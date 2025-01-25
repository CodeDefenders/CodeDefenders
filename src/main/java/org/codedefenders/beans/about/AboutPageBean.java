package org.codedefenders.beans.about;

import org.codedefenders.util.VersionUtils;

public class AboutPageBean {
    private final String version;
    private final boolean dirty;

    public AboutPageBean() {
        version = VersionUtils.getCodeDefendersVersion();
        dirty = VersionUtils.getGitDirty();
    }

    public String getVersion() {
        return version;
    }

    public boolean isDirty() {
        return dirty;
    }
}
