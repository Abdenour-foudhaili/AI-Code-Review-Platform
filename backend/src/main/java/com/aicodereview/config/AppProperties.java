package com.aicodereview.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String frontendUrl = "http://localhost:4200";
    private Security security = new Security();

    @Data
    public static class Security {
        private String jwtSecret = "";
        private int jwtExpirationMinutes = 15;
        private int refreshExpirationDays = 7;
        private boolean cookieSecure = false;
        private String tokenEncryptionKey = "";
        private String adminEmail = "";
        private String adminPassword = "";
    }
}
