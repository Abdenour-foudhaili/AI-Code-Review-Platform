package com.aicodereview.github;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GithubApiClientTest {
    @Test
    void acceptsNormalRepositoryNames() {
        assertDoesNotThrow(() -> GithubApiClient.validateRepoName("octocat", "Hello-World.git"));
    }

    @Test
    void rejectsPathTraversalAndInjection() {
        assertThrows(IllegalArgumentException.class, () -> GithubApiClient.validateRepoName("..", "repo/../x"));
        assertThrows(IllegalArgumentException.class, () -> GithubApiClient.validateRepoName("owner", "repo?x=1"));
        assertThrows(IllegalArgumentException.class, () -> GithubApiClient.validateRepoName("owner/evil", "repo"));
        assertThrows(IllegalArgumentException.class, () -> GithubApiClient.validateRepoName(null, "repo"));
    }
}
