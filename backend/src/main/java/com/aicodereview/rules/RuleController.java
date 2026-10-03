package com.aicodereview.rules;

import com.aicodereview.rules.RuleDtos.RuleRequest;
import com.aicodereview.rules.RuleDtos.RuleResponse;
import com.aicodereview.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rules")
public class RuleController {
    private final RuleService service;

    public RuleController(RuleService service) { this.service = service; }

    @GetMapping
    public List<RuleResponse> list(@AuthenticationPrincipal AuthenticatedUser user) { return service.list(user.id()); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RuleResponse create(@Valid @RequestBody RuleRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.create(user.id(), request);
    }

    @PutMapping("/{id}")
    public RuleResponse update(@PathVariable Long id, @Valid @RequestBody RuleRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return service.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser user) { service.delete(user.id(), id); }
}
