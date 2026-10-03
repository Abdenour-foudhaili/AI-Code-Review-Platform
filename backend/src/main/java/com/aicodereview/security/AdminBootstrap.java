package com.aicodereview.security;

import com.aicodereview.config.AppProperties;
import com.aicodereview.entity.Role;
import com.aicodereview.entity.User;
import com.aicodereview.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first ADMIN account from ADMIN_EMAIL / ADMIN_PASSWORD (only if both are set and the account does not exist). */
@Component
public class AdminBootstrap implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);
    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AdminBootstrap(AppProperties props, UserRepository users, PasswordEncoder encoder) {
        this.props = props;
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        String email = props.getSecurity().getAdminEmail();
        String password = props.getSecurity().getAdminPassword();
        if (email == null || email.isBlank() || password == null || password.length() < 8) return;
        if (users.existsByEmailIgnoreCase(email)) return;
        String base = "admin";
        String username = base;
        int i = 1;
        while (users.existsByUsernameIgnoreCase(username)) username = base + (++i);
        users.save(User.builder().username(username).email(email.trim().toLowerCase())
                .password(encoder.encode(password)).role(Role.ADMIN).enabled(true).build());
        log.info("Bootstrap ADMIN account created for {}", email);
    }
}
