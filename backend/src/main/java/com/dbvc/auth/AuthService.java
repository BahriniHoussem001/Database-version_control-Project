package com.dbvc.auth;

import com.dbvc.auth.dto.LoginRequest;
import com.dbvc.auth.dto.LoginResponse;
import com.dbvc.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsernameIgnoreCase(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        JwtService.GeneratedToken generatedToken = jwtService.generateToken(user);

        return new LoginResponse(
                generatedToken.token(),
                "Bearer",
                user.getUsername(),
                user.getRole(),
                generatedToken.expiresAt()
        );
    }
}