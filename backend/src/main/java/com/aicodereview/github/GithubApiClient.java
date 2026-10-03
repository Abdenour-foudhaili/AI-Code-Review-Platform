package com.aicodereview.github;

import com.aicodereview.exception.ExternalServiceException;
import com.aicodereview.exception.ResourceNotFoundException;
import com.aicodereview.github.GithubDtos.GithubRepo;
import com.aicodereview.github.GithubDtos.GithubUser;
import com.aicodereview.github.GithubDtos.OAuthToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Thin GitHub REST client. Tokens are only used as Authorization headers: never logged, never put in messages. */
@Component
public class GithubApiClient {
    private static final String API = "https://api.github.com";
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_.-]{1,100}$");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER)   // redirects are followed manually (host allow-list, no token forwarded)
            .build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final GithubProperties props;

    public GithubApiClient(GithubProperties props) { this.props = props; }

    public static void validateRepoName(String owner, String repo) {
        if (owner == null || repo == null || !SAFE_NAME.matcher(owner).matches() || !SAFE_NAME.matcher(repo).matches()) {
            throw new IllegalArgumentException("Invalid repository name");
        }
    }

    // ---------------------------------------------------------------- OAuth

    public OAuthToken exchangeCode(String code) {
        String form = "client_id=" + enc(props.getClientId()) + "&client_secret=" + enc(props.getClientSecret())
                + "&code=" + enc(code) + "&redirect_uri=" + enc(props.getRedirectUri());
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://github.com/login/oauth/access_token"))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        JsonNode json = parse(send(request).body());
        if (json.hasNonNull("error") || !json.hasNonNull("access_token")) {
            throw new ExternalServiceException("GitHub OAuth failed: " + json.path("error").asText("unknown_error"), HttpStatus.BAD_GATEWAY);
        }
        return new OAuthToken(json.path("access_token").asText(), json.path("scope").asText(""));
    }

    public GithubUser fetchUser(String token) {
        JsonNode u = getJson(token, "/user");
        return new GithubUser(u.path("id").asLong(), u.path("login").asText(), u.path("avatar_url").asText(""));
    }

    // ---------------------------------------------------------------- repositories

    public List<GithubRepo> listRepositories(String token, int page, int perPage) {
        int pp = Math.min(Math.max(perPage, 1), 100);
        int p = Math.max(page, 1);
        JsonNode arr = getJson(token, "/user/repos?sort=updated&per_page=" + pp + "&page=" + p
                + "&affiliation=owner,collaborator,organization_member");
        List<GithubRepo> repos = new ArrayList<>();
        if (arr.isArray()) arr.forEach(n -> repos.add(toRepo(n)));
        return repos;
    }

    public GithubRepo getRepository(String token, String owner, String repo) {
        validateRepoName(owner, repo);
        return toRepo(getJson(token, "/repos/" + owner + "/" + repo));
    }

    public String resolveCommitSha(String token, String owner, String repo, String ref) {
        validateRepoName(owner, repo);
        JsonNode c = getJson(token, "/repos/" + owner + "/" + repo + "/commits/" + encPath(ref));
        return c.path("sha").asText("");
    }

    /** Downloads the repository archive of {@code ref} (branch, tag or sha) in memory, capped at {@code maxBytes}. */
    public byte[] downloadZip(String token, String owner, String repo, String ref, long maxBytes) {
        validateRepoName(owner, repo);
        HttpRequest first = authed(token, "/repos/" + owner + "/" + repo + "/zipball/" + encPath(ref)).build();
        HttpResponse<InputStream> response = sendStream(first);
        try {
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                String location = response.headers().firstValue("Location").orElse("");
                response.body().close();
                URI target = URI.create(location);
                String host = target.getHost() == null ? "" : target.getHost().toLowerCase();
                if (!"https".equalsIgnoreCase(target.getScheme())
                        || !(host.equals("codeload.github.com") || host.endsWith(".githubusercontent.com") || host.equals("github.com"))) {
                    throw new ExternalServiceException("Unexpected archive location returned by GitHub");
                }
                // second hop: no Authorization header (the signed URL carries its own authorization)
                response = sendStream(HttpRequest.newBuilder(target).timeout(Duration.ofMinutes(5)).header("User-Agent", "ai-code-review-platform").GET().build());
            }
            checkStatus(response.statusCode());
            return readLimited(response.body(), maxBytes);
        } catch (IOException e) {
            throw new ExternalServiceException("Could not download the repository archive");
        }
    }

    // ---------------------------------------------------------------- internals

    private GithubRepo toRepo(JsonNode n) {
        return new GithubRepo(n.path("id").asLong(), n.path("name").asText(), n.path("full_name").asText(),
                n.path("owner").path("login").asText(), n.path("description").isNull() ? "" : n.path("description").asText(""),
                n.path("language").isNull() ? "" : n.path("language").asText(""),
                n.path("stargazers_count").asInt(0), n.path("private").asBoolean(false), n.path("updated_at").asText(""),
                n.path("default_branch").asText("main"), n.path("html_url").asText(""));
    }

    private HttpRequest.Builder authed(String token, String path) {
        return HttpRequest.newBuilder(URI.create(API + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .header("User-Agent", "ai-code-review-platform")
                .GET();
    }

    private JsonNode getJson(String token, String path) {
        HttpResponse<String> r = send(authed(token, path).build());
        checkStatus(r.statusCode());
        return parse(r.body());
    }

    private void checkStatus(int status) {
        if (status >= 200 && status < 300) return;
        switch (status) {
            case 401 -> throw new ExternalServiceException("GitHub access was revoked or expired. Please reconnect GitHub.", HttpStatus.FAILED_DEPENDENCY);
            case 403, 429 -> throw new ExternalServiceException("GitHub refused the request (rate limit or insufficient permissions).", HttpStatus.TOO_MANY_REQUESTS);
            case 404 -> throw new ResourceNotFoundException("Repository or branch not found, or not accessible with your GitHub connection");
            default -> throw new ExternalServiceException("GitHub returned HTTP " + status);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Interrupted while contacting GitHub");
        } catch (IOException e) {
            throw new ExternalServiceException("Could not reach GitHub");
        }
    }

    private HttpResponse<InputStream> sendStream(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Interrupted while contacting GitHub");
        } catch (IOException e) {
            throw new ExternalServiceException("Could not reach GitHub");
        }
    }

    private static byte[] readLimited(InputStream in, long maxBytes) throws IOException {
        try (in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[16 * 1024];
            long total = 0;
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > maxBytes) {
                    throw new ExternalServiceException("Repository is too large to analyse (limit " + (maxBytes / 1024 / 1024) + " MB)", HttpStatus.PAYLOAD_TOO_LARGE);
                }
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        }
    }

    private JsonNode parse(String body) {
        try {
            return mapper.readTree(body);
        } catch (IOException e) {
            throw new ExternalServiceException("Invalid response from GitHub");
        }
    }

    private static String enc(String s) { return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8); }

    private static String encPath(String ref) {
        return URLEncoder.encode(ref == null ? "" : ref, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
