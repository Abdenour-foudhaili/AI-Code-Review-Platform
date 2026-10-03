package com.aicodereview.rules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

public final class RuleDtos {
    private RuleDtos() {}

    @Data
    public static class RuleRequest {
        @NotBlank(message = "Rule name is required")
        @Size(max = 120, message = "Rule name must be at most 120 characters")
        private String name;

        @Size(max = 500, message = "Description must be at most 500 characters")
        private String description;

        @NotBlank(message = "Rule text is required")
        @Size(max = 2000, message = "Rule text must be at most 2000 characters")
        private String ruleText;

        private Boolean enabled;
    }

    public record RuleResponse(Long id, String name, String description, String ruleText, boolean enabled, LocalDateTime createdAt) {}
}
