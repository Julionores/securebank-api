package com.jtmcloud.securebank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée de l'API SecureBank.
 *
 * Exemple de projet démontrant une architecture Full Stack bancaire sécurisée :
 * authentification JWT, contrôle d'accès par ressource, validation stricte des entrées,
 * journalisation d'audit et conformité aux principes OWASP / PCI-DSS.
 */
@SpringBootApplication
public class SecureBankApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureBankApiApplication.class, args);
    }
}
