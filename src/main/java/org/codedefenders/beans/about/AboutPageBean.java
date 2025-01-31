package org.codedefenders.beans.about;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

import org.codedefenders.util.VersionUtils;

@Named("aboutPage")
@ApplicationScoped
public class AboutPageBean {
    private final String gitCommitHash;
    private final String version;
    private final boolean dirty;

    public AboutPageBean() {
        gitCommitHash = VersionUtils.getGitCommitId();
        dirty = VersionUtils.getGitDirty();
        version = VersionUtils.getCodeDefendersVersion();
    }

    public String getGitCommitHash() {
        return gitCommitHash;
    }

    public boolean isDirty() {
        return dirty;
    }

    public String getVersion() {
        return version;
    }
}
