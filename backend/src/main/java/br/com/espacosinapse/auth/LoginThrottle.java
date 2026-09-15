package br.com.espacosinapse.auth;

import br.com.espacosinapse.common.ApiException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginThrottle {
    private static final int MAX_TRACKED_ATTEMPTS = 10_000;
    private static final int HARD_ATTEMPT_LIMIT = 20_000;
    private static final long WINDOW_SECONDS = 900;

    private record Window(Instant expires, int attempts) {
    }

    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

    public void check(String remoteIp, String email) {
        checkKey("ip:" + remoteIp, 40);
        checkKey("email:" + email.toLowerCase(Locale.ROOT), 8);
    }

    private void checkKey(String key, int limit) {
        Instant now = Instant.now();

        if (attempts.size() > MAX_TRACKED_ATTEMPTS) {
            attempts.entrySet().removeIf(entry -> entry.getValue().expires().isBefore(now));
        }

        if (attempts.size() > HARD_ATTEMPT_LIMIT) {
            throw new ApiException(429, "Muitas tentativas. Aguarde alguns minutos.");
        }

        Window window = attempts.compute(key, (attemptKey, currentWindow) -> {
            if (currentWindow == null || currentWindow.expires().isBefore(now)) {
                return new Window(now.plusSeconds(WINDOW_SECONDS), 1);
            }

            return new Window(currentWindow.expires(), currentWindow.attempts() + 1);
        });
        if (window.attempts() > limit) {
            throw new ApiException(429, "Muitas tentativas de entrada. Aguarde 15 minutos.");
        }
    }

    public void success(String email) {
        attempts.remove("email:" + email.toLowerCase(Locale.ROOT));
    }
}
