package com.aicodereview.github;

import com.aicodereview.config.AppProperties;
import com.aicodereview.github.GithubDtos.ConnectUrl;
import com.aicodereview.github.GithubDtos.GithubRepo;
import com.aicodereview.github.GithubDtos.GithubStatus;
import com.aicodereview.security.AuthenticatedUser;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/github")
public class GithubController {
    private static final Logger log = LoggerFactory.getLogger(GithubController.class);

    private final GithubConnectionService connectionService;
    private final GithubApiClient api;
    private final AppProperties props;

    public GithubController(GithubConnectionService connectionService, GithubApiClient api, AppProperties props) {
        this.connectionService = connectionService;
        this.api = api;
        this.props = props;
    }

    @GetMapping("/status")
    public GithubStatus status(@AuthenticationPrincipal AuthenticatedUser user) { return connectionService.status(user.id()); }

    /** The SPA calls this (with its JWT) and then navigates the browser to the returned GitHub URL. */
    @GetMapping("/connect-url")
    public ConnectUrl connectUrl(@AuthenticationPrincipal AuthenticatedUser user) {
        return new ConnectUrl(connectionService.buildAuthorizeUrl(user.id()));
    }

    /** OAuth callback (public: the browser comes from github.com). Identity comes from the signed "state". */
    @GetMapping("/callback")
    public void callback(@RequestParam(required = false) String code, @RequestParam(required = false) String state,
                         @RequestParam(required = false) String error, HttpServletResponse response) throws IOException {
        String base = props.getFrontendUrl().split(",")[0].trim().replaceAll("/+$", "");
        if (error != null || code == null || code.isBlank() || state == null || state.isBlank()) {
            response.sendRedirect(base + "/settings?github=error");
            return;
        }
        try {
            connectionService.handleCallback(code, state);
            response.sendRedirect(base + "/repositories?github=connected");
        } catch (Exception e) {
            log.warn("GitHub OAuth callback failed: {}", e.getClass().getSimpleName());   // never log the code / token
            response.sendRedirect(base + "/settings?github=error");
        }
    }

    @GetMapping("/repositories")
    public List<GithubRepo> repositories(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "50") int perPage,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return api.listRepositories(connectionService.requireToken(user.id()), page, perPage);
    }

    @GetMapping("/repositories/{owner}/{repo}")
    public GithubRepo repository(@PathVariable String owner, @PathVariable String repo, @AuthenticationPrincipal AuthenticatedUser user) {
        return api.getRepository(connectionService.requireToken(user.id()), owner, repo);
    }

    @DeleteMapping("/connection")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disconnect(@AuthenticationPrincipal AuthenticatedUser user) { connectionService.disconnect(user.id()); }
}
