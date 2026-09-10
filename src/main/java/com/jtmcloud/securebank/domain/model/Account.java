package com.jtmcloud.securebank.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Compte bancaire d'un client. Le solde utilise BigDecimal (jamais double/float) pour
 * éviter les erreurs d'arrondi monétaire. Le champ {@code version} active le verrouillage
 * optimiste JPA afin d'éviter les conditions de concurrence lors de virements simultanés.
 */
@Entity
@Table(name = "account")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 34)
    private String iban;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "XAF";

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
