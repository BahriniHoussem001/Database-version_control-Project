package com.dbvc.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AuthDataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String defaultAdminUsername;
    private final String defaultAdminPassword;

    public AuthDataInitializer(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${dbvc.auth.default-admin-username}") String defaultAdminUsername,
            @Value("${dbvc.auth.default-admin-password}") String defaultAdminPassword
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultAdminUsername = defaultAdminUsername;
        this.defaultAdminPassword = defaultAdminPassword;
    }

    @Override
    public void run(String... args) {
        if (appUserRepository.existsByUsernameIgnoreCase(defaultAdminUsername)) {
            return;
        }

        AppUser admin = new AppUser();
        admin.setUsername(defaultAdminUsername);
        admin.setPasswordHash(passwordEncoder.encode(defaultAdminPassword));
        admin.setRole("ADMIN");
        admin.setEnabled(1);
        admin.setCreatedAt(LocalDateTime.now());

        appUserRepository.save(admin);
    }
}