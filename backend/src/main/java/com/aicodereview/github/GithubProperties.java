package com.aicodereview.github;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "github")
public class GithubProperties {
    private String clientId = "";
    private String clientSecret = "";
    private String redirectUri = "http://localhost:8081/api/github/callback";
    private String scope = "repo read:user";

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
