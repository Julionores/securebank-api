package com.jtmcloud.securebank.exception;

import com.jtmcloud.securebank.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Point unique de traduction des exceptions en réponses HTTP.
 *
 * Règle stricte (OWASP A05) : aucune exception non prévue ne doit jamais renvoyer son
 * message brut ou sa stack trace au client. Le détail complet est loggé côté serveur
 * uniquement ; le client ne reçoit qu'un message générique et un identifiant de niveau HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Requête invalide", req, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Corps de requête JSON invalide", req, List.of());
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Compte introuvable", req, List.of());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "Solde insuffisant", req, List.of());
    }

    @ExceptionHandler(UnauthorizedAccountAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedAccess(UnauthorizedAccountAccessException ex, HttpServletRequest req) {
        log.warn("Tentative d'accès non autorisé - path={} detail={}", req.getRequestURI(), ex.getMessage());
        return build(HttpStatus.FORBIDDEN, "Accès refusé", req, List.of());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex, HttpServletRequest req) {
        return build(HttpStatus.LOCKED, "Compte temporairement verrouillé", req, List.of());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ErrorResponse> handleBadCredentials(Exception ex, HttpServletRequest req) {
        // Message générique volontairement identique, que l'email n'existe pas ou que le
        // mot de passe soit faux (OWASP A07 - ne pas révéler l'énumération de comptes).
        return build(HttpStatus.UNAUTHORIZED, "Identifiants invalides", req, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Accès refusé", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        // Le détail complet part dans les logs serveur (corrélable par timestamp), jamais au client.
        log.error("Erreur non gérée sur {}", req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue", req, List.of());
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req, List<String> details) {
        ErrorResponse body = new ErrorResponse(
                Instant.now(), status.value(), status.getReasonPhrase(), message, req.getRequestURI(), details
        );
        return ResponseEntity.status(status).body(body);
    }
}
