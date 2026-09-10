package com.jtmcloud.securebank.domain.model;

/**
 * Rôles applicatifs. ROLE_CUSTOMER pour les clients, ROLE_ADMIN pour le personnel bancaire
 * (opérations de supervision uniquement — jamais d'accès direct aux fonds d'un client).
 */
public enum Role {
    ROLE_CUSTOMER,
    ROLE_ADMIN
}
