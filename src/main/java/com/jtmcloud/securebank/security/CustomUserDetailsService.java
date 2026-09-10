package com.jtmcloud.securebank.security;

import com.jtmcloud.securebank.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                // Message volontairement identique à celui d'un mauvais mot de passe
                // (voir AuthService) pour ne pas révéler l'existence d'un compte (OWASP A07).
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));
    }
}
