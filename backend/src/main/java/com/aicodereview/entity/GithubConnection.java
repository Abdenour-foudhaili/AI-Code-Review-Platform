package com.aicodereview.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "github_connections", uniqueConstraints = @UniqueConstraint(name = "uk_github_user", columnNames = "user_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GithubConnection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private String githubLogin;
    private Long githubUserId;
    private String avatarUrl;
    private String scopes;

    /** AES-256-GCM encrypted GitHub access token (Base64). Never returned by the API, never logged. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String encryptedAccessToken;

    private LocalDateTime connectedAt;
}
