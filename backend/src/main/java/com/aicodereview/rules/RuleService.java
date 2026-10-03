package com.aicodereview.rules;

import com.aicodereview.entity.ReviewRule;
import com.aicodereview.exception.ResourceNotFoundException;
import com.aicodereview.repository.ReviewRuleRepository;
import com.aicodereview.repository.UserRepository;
import com.aicodereview.rules.RuleDtos.RuleRequest;
import com.aicodereview.rules.RuleDtos.RuleResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RuleService {
    private final ReviewRuleRepository rules;
    private final UserRepository users;

    public RuleService(ReviewRuleRepository rules, UserRepository users) {
        this.rules = rules;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> list(Long userId) {
        return rules.findByOwnerIdOrderByCreatedAtDesc(userId).stream().map(RuleService::toResponse).toList();
    }

    @Transactional
    public RuleResponse create(Long userId, RuleRequest req) {
        ReviewRule rule = ReviewRule.builder()
                .owner(users.getReferenceById(userId))
                .name(req.getName().trim())
                .description(req.getDescription())
                .ruleText(req.getRuleText().trim())
                .enabled(req.getEnabled() == null || req.getEnabled())
                .createdAt(LocalDateTime.now())
                .build();
        return toResponse(rules.save(rule));
    }

    @Transactional
    public RuleResponse update(Long userId, Long id, RuleRequest req) {
        ReviewRule rule = rules.findByIdAndOwnerId(id, userId).orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        rule.setName(req.getName().trim());
        rule.setDescription(req.getDescription());
        rule.setRuleText(req.getRuleText().trim());
        if (req.getEnabled() != null) rule.setEnabled(req.getEnabled());
        return toResponse(rules.save(rule));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        ReviewRule rule = rules.findByIdAndOwnerId(id, userId).orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));
        rules.delete(rule);
    }

    /** Texts of the enabled rules, injected in the AI prompts. */
    @Transactional(readOnly = true)
    public List<String> enabledRuleTexts(Long userId) {
        return rules.findByOwnerIdAndEnabledTrue(userId).stream().map(r -> r.getName() + ": " + r.getRuleText()).toList();
    }

    static RuleResponse toResponse(ReviewRule r) {
        return new RuleResponse(r.getId(), r.getName(), r.getDescription(), r.getRuleText(), r.isEnabled(), r.getCreatedAt());
    }
}
