package fi.natroutter.foxlib.updates;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.updates.data.GitHubRelease;
import fi.natroutter.foxlib.updates.data.UpdateStatus;
import fi.natroutter.foxlib.updates.data.VersionInfo;
import lombok.Setter;
import org.jsoup.Jsoup;
import org.jsoup.Connection;

import java.util.concurrent.CompletableFuture;

public class GitHubVersionChecker {

    private final String repoOwner;
    private final String repoName;
    private final String currentVersion;
    private final Gson gson;

    @Setter
    private FoxLogger logger = new FoxLogger.Builder()
            .setDebug(false)
            .setPruneOlderThanDays(35)
            .setSaveIntervalSeconds(300)
            .setLoggerName("GitHubVersionChecker")
            .build();

    public GitHubVersionChecker(String repoOwner, String repoName, String currentVersion) {
        this.repoOwner = repoOwner;
        this.repoName = repoName;
        this.currentVersion = currentVersion;
        this.gson = new Gson();
    }

    /**
     * Check for updates asynchronously
     * @return CompletableFuture with VersionInfo
     */
    public CompletableFuture<VersionInfo> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String apiUrl = String.format("https://api.github.com/repos/%s/%s/releases/latest",
                        repoOwner, repoName);

                // Use Jsoup to fetch the JSON response and check status
                Connection.Response response = Jsoup.connect(apiUrl)
                        .header("Accept", "application/vnd.github.v3+json")
                        .ignoreContentType(true)
                        .timeout(5000)
                        .execute();

                // Check if status is 200
                if (response.statusCode() != 200) {
                    logger.error("GitHub API returned status code: " + response.statusCode());
                    return new VersionInfo(currentVersion, currentVersion, UpdateStatus.ERROR, "", "", "", "");
                }

                String jsonResponse = response.body();

                // Parse with Gson
                GitHubRelease release = gson.fromJson(jsonResponse, GitHubRelease.class);

                String latestVersion = release.getTagName();
                // Remove 'v' prefix if present (e.g., "v1.0.0" -> "1.0.0")
                latestVersion = latestVersion.startsWith("v") ? latestVersion.substring(1) : latestVersion;

                UpdateStatus updateAvailable = isNewerVersion(currentVersion, latestVersion) ? UpdateStatus.YES : UpdateStatus.NO;

                return new VersionInfo(
                        currentVersion,
                        latestVersion,
                        updateAvailable,
                        release.getName() != null ? release.getName() : "",
                        release.getHtmlUrl() != null ? release.getHtmlUrl() : "",
                        release.getBody() != null ? release.getBody() : "",
                        release.getPublishedAt() != null ? release.getPublishedAt() : ""
                );
            } catch (Exception e) {
                logger.error("Failed to check for updates: " + e.getMessage());
                return new VersionInfo(currentVersion, currentVersion, UpdateStatus.ERROR, "", "", "", "");
            }
        });
    }

    /**
     * Compare version strings (supports semantic versioning: X.Y.Z)
     */
    private boolean isNewerVersion(String current, String latest) {
        try {
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");

            int length = Math.max(currentParts.length, latestParts.length);
            for (int i = 0; i < length; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (latestPart > currentPart) {
                    return true;
                } else if (latestPart < currentPart) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}