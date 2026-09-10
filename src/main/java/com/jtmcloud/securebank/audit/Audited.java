package com.jtmcloud.securebank.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque une méthode de service comme action sensible devant être tracée dans la table
 * d'audit (authentification, virement, etc.). Voir {@link AuditAspect}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Audited {
    String action();
}
