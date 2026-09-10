package com.jtmcloud.securebank.dto;

import java.time.Instant;
import java.util.List;

/**
 * Format d'erreur volontairement générique : jamais de stack trace, de nom de classe
 * interne, de requête SQL ou de détail d'implémentation renvoyé au client
 * (OWASP A05 - Security Misconfiguration / exposition d'informations sensibles).
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> details
) {
}
