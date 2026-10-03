package com.aicodereview.github;

import com.aicodereview.entity.GithubConnection;
import com.aicodereview.entity.User;
import com.aicodereview.exception.ExternalServiceException;
import com.aicodereview.github.GithubDtos.GithubStatus;
import com.aicodereview.github.GithubDtos.GithubUser;
import com.aicodereview.github.GithubDtos.OAuthToken;
import com.aicodereview.repository.GithubConnectionRepository;
import com.aicodereview.repository.UserRepository;
import com.aicodereview.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class GithubConnectionService {
    private final GithubProperties props;
    private final GithubApiClient api;
    private final GithubConnectionRepository connections;
    private final UserRepository users;
    private final JwtService jwtService;
    private final TokenCrypto crypto;

    public GithubConnectionService(GithubProperties props, GithubApiClient api, GithubConnectionRepository connections,
                                   UserRepository users, JwtService jwtService, TokenCrypto crypto) {
        this.props = props;
        this.api = api;
        this.connections = connections;
        this.users = users;
        this.jwtService = jwtService;
        this.crypto = crypto;
    }

    /** Authorization URL to which the browser is redirected. "state" is a signed, 10-minute value bound to the user. */
    public String buildAuthorizeUrl(Long userId) {
        if (!props.isConfigured()) {
            throw new ExternalServiceException("GitHub OAuth is not configured on the server (GITHUB_CLIENT_ID / GITHUB_CLIENT_SECRET)", HttpStatus.SERVICE_UNAVAILABLE);
        }
        return "https://github.com/login/oauth/authorize"
                + "?client_id=" + enc(props.getClientId())
                + "&redirect_uri=" + enc(props.getRedirectUri())
                + "&scope=" + enc(props.getScope())
                + "&state=" + enc(jwtService.createGithubState(userId))
                + "&allow_signup=false";
    }

    @Transactional
    public void handleCallback(String code, String state) {
        Long userId = jwtService.parseGithubState(state);       // throws JwtException when forged/expired
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Unknown user"));
        OAuthToken token = api.exchangeCode(code);
        GithubUser ghUser = api.fetchUser(token.accessToken());
        GithubConnection connection = connections.findByUserId(userId).orElseGet(() -> GithubConnection.builder().user(user).build());
        connection.setGithubLogin(ghUser.login());
        connection.setGithubUserId(ghUser.id());
        connection.setAvatarUrl(ghUser.avatarUrl());
        connection.setScopes(token.scope());
        connection.setEncryptedAccessToken(crypto.encrypt(token.accessToken()));
        connection.setConnectedAt(LocalDateTime.now());
        connections.save(connection);
    }

    @Transactional(readOnly = true)
    public GithubStatus status(Long userId) {
        return connections.findByUserId(userId)
                .map(c -> new GithubStatus(props.isConfigured(), true, c.getGithubLogin(), c.getAvatarUrl(), c.getConnectedAt()))
                .orElse(new GithubStatus(props.isConfigured(), false, null, null, null));
    }

    @Transactional(readOnly = true)
    public String requireToken(Long userId) {
        GithubConnection c = connections.findByUserId(userId).orElseThrow(() ->
                new ExternalServiceException("GitHub is not connected. Connect your GitHub account first.", HttpStatus.FAILED_DEPENDENCY));
        return crypto.decrypt(c.getEncryptedAccessToken());
    }

    @Transactional
    public void disconnect(Long userId) { connections.deleteByUserId(userId); }

    private static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
