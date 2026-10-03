package com.aicodereview.github;

import java.time.LocalDateTime;

/** Response/value types of the GitHub module. The access token is intentionally absent from all of them. */
public final class GithubDtos {
    private GithubDtos() {}

    public record GithubStatus(boolean configured, boolean connected, String login, String avatarUrl, LocalDateTime connectedAt) {}

    public record GithubRepo(long id, String name, String fullName, String owner, String description, String language,
                             int stars, boolean isPrivate, String updatedAt, String defaultBranch, String htmlUrl) {}

    public record GithubUser(long id, String login, String avatarUrl) {}

    public record OAuthToken(String accessToken, String scope) {}

    public record ConnectUrl(String url) {}

    public record AnalysisStarted(Long analysisId, String status) {}
}
