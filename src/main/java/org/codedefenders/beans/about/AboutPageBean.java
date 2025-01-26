package org.codedefenders.beans.about;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.codedefenders.util.VersionUtils;

@Named("aboutPage")
@ApplicationScoped
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
