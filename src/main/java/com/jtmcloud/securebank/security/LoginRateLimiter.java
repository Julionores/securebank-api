package com.jtmcloud.securebank.security;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limiteur de tentatives de connexion en mémoire, par identifiant (email + adresse IP).
 * Contre-mesure OWASP A07 (brute-force / credential stuffing) simple et sans dépendance
 * externe, suffisante pour une démonstration ou un déploiement mono-instance.
 *
 * En production multi-instances, remplacer par un compteur partagé (Redis) afin que la
 * limite s'applique uniformément derrière un load balancer.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Attempts(int count, Instant windowStart) {
    }

    private final Map<String, Attempts> attemptsByKey = new ConcurrentHashMap<>();

    public boolean isBlocked(String key) {
        Attempts attempts = attemptsByKey.get(key);
        if (attempts == null) {
            return false;
        }
        if (Instant.now().isAfter(attempts.windowStart().plus(WINDOW))) {
            attemptsByKey.remove(key);
            return false;
        }
        return attempts.count() >= MAX_ATTEMPTS;
    }

    public void recordFailure(String key) {
        attemptsByKey.compute(key, (k, current) -> {
            Instant now = Instant.now();
            if (current == null || now.isAfter(current.windowStart().plus(WINDOW))) {
                return new Attempts(1, now);
            }
            return new Attempts(current.count() + 1, current.windowStart());
        });
    }

    public void recordSuccess(String key) {
        attemptsByKey.remove(key);
    }
}
