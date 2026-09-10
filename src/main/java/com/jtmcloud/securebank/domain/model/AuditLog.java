package com.jtmcloud.securebank.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Trace d'audit horodatée pour toute action sensible (authentification, virement,
 * consultation de compte tiers refusée, etc.). Alimentée automatiquement par
 * {@link com.jtmcloud.securebank.audit.AuditAspect} — jamais renseignée manuellement
 * dans la logique métier, afin de garantir sa cohérence et son exhaustivité.
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 150)
    private String actorEmail;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(length = 500)
    private String details;

    @Column(nullable = false)
    private boolean success;

    @Column(nullable = false, length = 45)
    private String sourceIp;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @PrePersist
    void onCreate() {
        this.occurredAt = Instant.now();
    }
}
