package fi.natroutter.foxlib.updates;

import com.google.gson.Gson;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.updates.data.GitHubRelease;
import fi.natroutter.foxlib.updates.data.UpdateStatus;
import fi.natroutter.foxlib.updates.data.VersionInfo;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.util.concurrent.CompletableFuture;

public class GitHubVersionChecker {

    private final String repoOwner;
    private final String repoName;
    private final String currentVersion;
    private final String authToken;
    private final String baseApiUrl;
    private final int timeout;
    private final boolean includePreReleases;
    private final boolean stripVersionPrefix;
    private final FoxLogger logger;
    private final Gson gson;

    private GitHubVersionChecker(Builder builder) {
        this.repoOwner = builder.repoOwner;
        this.repoName = builder.repoName;
        this.currentVersion = builder.currentVersion;
        this.authToken = builder.authToken;
        this.baseApiUrl = builder.baseApiUrl;
        this.timeout = builder.timeout;
        this.includePreReleases = builder.includePreReleases;
        this.stripVersionPrefix = builder.stripVersionPrefix;
        this.logger = builder.logger;
        this.gson = new Gson();
    }

    /**
     * Shorthand constructor using defaults. For advanced configuration, use {@link Builder}.
     */
    public GitHubVersionChecker(String repoOwner, String repoName, String currentVersion) {
        this(new Builder(repoOwner, repoName, currentVersion));
    }

    /**
     * Check for updates asynchronously using the configured timeout.
     *
     * @return CompletableFuture with VersionInfo
     */
    public CompletableFuture<VersionInfo> checkForUpdates() {
        return checkForUpdates(this.timeout);
    }

    /**
     * Check for updates asynchronously with an explicit timeout override.
     *
     * @param timeout request timeout in milliseconds
     * @return CompletableFuture with VersionInfo
     */
    public CompletableFuture<VersionInfo> checkForUpdates(int timeout) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                GitHubRelease release = includePreReleases
                        ? fetchLatestIncludingPreReleases(timeout)
                        : fetchLatestStableRelease(timeout);

                if (release == null) {
                    return errorResult();
                }

                String latestVersion = release.getTagName();
                if (stripVersionPrefix && latestVersion.startsWith("v")) {
                    latestVersion = latestVersion.substring(1);
                }

                UpdateStatus status = isNewerVersion(currentVersion, latestVersion)
                        ? UpdateStatus.YES : UpdateStatus.NO;

                return new VersionInfo(
                        currentVersion,
                        latestVersion,
                        status,
                        release.getName() != null ? release.getName() : "",
                        release.getHtmlUrl() != null ? release.getHtmlUrl() : "",
                        release.getBody() != null ? release.getBody() : "",
                        release.getPublishedAt() != null ? release.getPublishedAt() : ""
                );
            } catch (Exception e) {
                logger.error("Failed to check for updates: " + e.getMessage());
                return errorResult();
            }
        });
    }

    private GitHubRelease fetchLatestStableRelease(int timeout) throws Exception {
        String url = String.format("%s/repos/%s/%s/releases/latest", baseApiUrl, repoOwner, repoName);
        String body = get(url, timeout);
        if (body == null) return null;
        return gson.fromJson(body, GitHubRelease.class);
    }

    private GitHubRelease fetchLatestIncludingPreReleases(int timeout) throws Exception {
        String url = String.format("%s/repos/%s/%s/releases?per_page=1", baseApiUrl, repoOwner, repoName);
        String body = get(url, timeout);
        if (body == null) return null;
        GitHubRelease[] releases = gson.fromJson(body, GitHubRelease[].class);
        return (releases != null && releases.length > 0) ? releases[0] : null;
    }

    private String get(String url, int timeout) throws Exception {
        Connection conn = Jsoup.connect(url)
                .header("Accept", "application/vnd.github.v3+json")
                .ignoreContentType(true)
                .timeout(timeout);

        if (authToken != null && !authToken.isEmpty()) {
            conn.header("Authorization", "Bearer " + authToken);
        }

        Connection.Response response = conn.execute();

        if (response.statusCode() != 200) {
            logger.error("GitHub API returned status code: " + response.statusCode());
            return null;
        }

        return response.body();
    }

    private VersionInfo errorResult() {
        return new VersionInfo(currentVersion, currentVersion, UpdateStatus.ERROR, "", "", "", "");
    }

    /**
     * Compare version strings (supports semantic versioning: X.Y.Z).
     */
    private boolean isNewerVersion(String current, String latest) {
        try {
            String[] currentParts = current.split("\\.");
            String[] latestParts = latest.split("\\.");

            int length = Math.max(currentParts.length, latestParts.length);
            for (int i = 0; i < length; i++) {
                int currentPart = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                int latestPart = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;

                if (latestPart > currentPart) return true;
                if (latestPart < currentPart) return false;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------------------

    public static class Builder {

        private final String repoOwner;
        private final String repoName;
        private final String currentVersion;

        private String authToken = null;
        private String baseApiUrl = "https://api.github.com";
        private int timeout = 15000;
        private boolean includePreReleases = false;
        private boolean stripVersionPrefix = true;
        private FoxLogger logger = new FoxLogger.Builder()
                .setDebug(false)
                .setPruneOlderThanDays(35)
                .setSaveIntervalSeconds(300)
                .setLoggerName("GitHubVersionChecker")
                .build();

        /**
         * @param repoOwner      GitHub repository owner or organisation
         * @param repoName       GitHub repository name
         * @param currentVersion currently running version of the plugin/application
         */
        public Builder(String repoOwner, String repoName, String currentVersion) {
            this.repoOwner = repoOwner;
            this.repoName = repoName;
            this.currentVersion = currentVersion;
        }

        /**
         * GitHub personal access token. Increases the API rate limit from 60 to 5 000
         * requests per hour.
         */
        public Builder authToken(String token) {
            this.authToken = token;
            return this;
        }

        /**
         * Override the GitHub API base URL. Useful for GitHub Enterprise instances.
         * Default: {@code https://api.github.com}
         */
        public Builder baseApiUrl(String url) {
            this.baseApiUrl = url;
            return this;
        }

        /**
         * Default HTTP timeout for update requests in milliseconds.
         * Default: {@code 15000}
         */
        public Builder timeout(int milliseconds) {
            this.timeout = milliseconds;
            return this;
        }

        /**
         * When {@code true}, the checker fetches all releases (including pre-releases)
         * and returns the most recent one. When {@code false} (default), only the latest
         * stable release is considered.
         */
        public Builder includePreReleases(boolean include) {
            this.includePreReleases = include;
            return this;
        }

        /**
         * When {@code true} (default), a leading {@code v} is stripped from the tag name
         * before comparison (e.g. {@code v1.2.3} → {@code 1.2.3}).
         */
        public Builder stripVersionPrefix(boolean strip) {
            this.stripVersionPrefix = strip;
            return this;
        }

        /**
         * Provide a custom {@link FoxLogger} instance.
         */
        public Builder logger(FoxLogger logger) {
            this.logger = logger;
            return this;
        }

        public GitHubVersionChecker build() {
            return new GitHubVersionChecker(this);
        }
    }
}