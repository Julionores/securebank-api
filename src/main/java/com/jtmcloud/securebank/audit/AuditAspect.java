package com.jtmcloud.securebank.audit;

import com.jtmcloud.securebank.domain.model.AuditLog;
import com.jtmcloud.securebank.domain.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Intercepte toute méthode annotée {@link Audited} et écrit une trace d'audit,
 * que l'opération réussisse ou échoue. Centraliser la traçabilité ici (plutôt que dans
 * chaque service) garantit qu'aucune action sensible ne peut être ajoutée sans être
 * automatiquement auditée, et qu'aucun code métier ne peut oublier de le faire.
 *
 * Champs volontairement exclus des logs : mot de passe, jeton JWT, numéro de carte.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        String actor = currentActorEmail();
        String ip = currentClientIp();
        try {
            Object result = joinPoint.proceed();
            save(actor, audited.action(), null, true, ip);
            return result;
        } catch (Exception ex) {
            save(actor, audited.action(), ex.getClass().getSimpleName(), false, ip);
            throw ex;
        }
    }

    private void save(String actor, String action, String details, boolean success, String ip) {
        auditLogRepository.save(AuditLog.builder()
                .actorEmail(actor)
                .action(action)
                .details(details)
                .success(success)
                .sourceIp(ip)
                .build());
    }

    private String currentActorEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
    }

    private String currentClientIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return "unknown";
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
