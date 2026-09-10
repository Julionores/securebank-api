package com.jtmcloud.securebank.exception;

/**
 * Levée quand un utilisateur authentifié tente d'accéder à un compte dont il n'est pas
 * propriétaire (contrôle d'accès au niveau objet - OWASP A01 Broken Access Control).
 */
public class UnauthorizedAccountAccessException extends RuntimeException {
    public UnauthorizedAccountAccessException(String message) {
        super(message);
    }
}
