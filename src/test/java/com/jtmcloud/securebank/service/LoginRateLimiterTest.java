package com.jtmcloud.securebank.service;

import com.jtmcloud.securebank.security.LoginRateLimiter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRateLimiterTest {

    @Test
    void blocksAfterFiveFailedAttempts() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String key = "victim@securebank.test|127.0.0.1";

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(key);
            assertThat(limiter.isBlocked(key)).isFalse();
        }

        limiter.recordFailure(key); // 5e échec
        assertThat(limiter.isBlocked(key)).isTrue();
    }

    @Test
    void successResetsCounter() {
        LoginRateLimiter limiter = new LoginRateLimiter();
        String key = "user@securebank.test|127.0.0.1";

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(key);
        }
        limiter.recordSuccess(key);

        assertThat(limiter.isBlocked(key)).isFalse();
    }
}
