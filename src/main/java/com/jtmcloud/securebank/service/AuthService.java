package com.jtmcloud.securebank.service;

import com.jtmcloud.securebank.audit.Audited;
import com.jtmcloud.securebank.domain.model.Account;
import com.jtmcloud.securebank.domain.model.Role;
import com.jtmcloud.securebank.domain.model.User;
import com.jtmcloud.securebank.domain.repository.AccountRepository;
import com.jtmcloud.securebank.domain.repository.UserRepository;
import com.jtmcloud.securebank.dto.AuthResponse;
import com.jtmcloud.securebank.dto.LoginRequest;
import com.jtmcloud.securebank.dto.RegisterRequest;
import com.jtmcloud.securebank.exception.AccountLockedException;
import com.jtmcloud.securebank.exception.EmailAlreadyRegisteredException;
import com.jtmcloud.securebank.security.JwtService;
import com.jtmcloud.securebank.security.LoginRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter rateLimiter;
    private final AuthenticationManager authenticationManager;

    @Transactional
    @Audited(action = "USER_REGISTER")
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new EmailAlreadyRegisteredException("Un compte existe déjà pour cette adresse email.");
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(Role.ROLE_CUSTOMER)
                .build();
        userRepository.save(user);

        Account account = Account.builder()
                .iban(generateDemoIban())
                .owner(user)
                .balance(BigDecimal.ZERO)
                .currency("XAF")
                .build();
        accountRepository.save(account);

        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds());
    }

    @Audited(action = "USER_LOGIN")
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.email().toLowerCase();
        String rateLimitKey = email + "|" + httpRequest.getRemoteAddr();
        if (rateLimiter.isBlocked(rateLimitKey)) {
            throw new AccountLockedException("Trop de tentatives échouées, réessayez plus tard.");
        }

        // Délègue à Spring Security (DaoAuthenticationProvider -> CustomUserDetailsService +
        // PasswordEncoder) plutôt que de comparer le mot de passe manuellement : on bénéficie
        // ainsi gratuitement des vérifications standard (compte désactivé, verrouillé, etc.).
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            rateLimiter.recordFailure(rateLimitKey);
            // Message générique unique, que l'email n'existe pas ou que le mot de passe soit
            // faux (OWASP A07 - pas d'énumération de comptes).
            throw new BadCredentialsException("Identifiants invalides");
        }

        rateLimiter.recordSuccess(rateLimitKey);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié introuvable: " + email));
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String token = jwtService.generateToken(user);
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds());
    }

    /** IBAN factice à des fins de démonstration - ne suit pas l'algorithme ISO 13616 complet. */
    private String generateDemoIban() {
        return "CM21" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
