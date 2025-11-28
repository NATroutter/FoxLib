package fi.natroutter.foxlib.updates.data;

import lombok.Data;

@Data
public class GitHubRelease {
    private String tag_name;
    private String name;
    private String html_url;
    private String body;
    private String published_at;

    // Getter methods with camelCase naming
    public String getTagName() { return tag_name; }
    public String getHtmlUrl() { return html_url; }
    public String getPublishedAt() { return published_at; }
}