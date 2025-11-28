package fi.natroutter.foxlib.updates.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class VersionInfo {
    private final String currentVersion;
    private final String latestVersion;
    private final UpdateStatus updateAvailable;
    private final String releaseName;
    private final String releaseUrl;
    private final String releaseNotes;
    private final String publishedAt;
}