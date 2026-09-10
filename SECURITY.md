# Politique de sécurité et mapping des contrôles

Ce document explicite les décisions de sécurité de SecureBank API et leur correspondance avec
l'**OWASP Top 10 (2021)** et les principes de contrôle **PCI-DSS** pertinents pour une application
qui manipule des comptes et des mouvements de fonds. Objectif : rendre auditable, en un coup
d'œil, ce qui est couvert et ce qui reste hors périmètre de ce projet.

## Mapping OWASP Top 10

| Risque OWASP | Contrôle implémenté | Où |
|---|---|---|
| A01 - Broken Access Control | Vérification de propriété sur **chaque** accès à un compte (pas seulement au niveau contrôleur) ; IDs opaques (UUID) plutôt que séquentiels | `AccountService.assertOwnership`, `AccountRepository` |
| A02 - Cryptographic Failures | Mots de passe hachés avec BCrypt (coût 12) ; TLS attendu en frontal (reverse proxy / ingress) ; secrets JWT et DB externalisés | `SecurityConfig`, `application.yml` |
| A03 - Injection | Aucune requête SQL concaténée : JPA/Hibernate avec requêtes paramétrées (`@Query` avec `:param`) | `AccountRepository` |
| A04 - Insecure Design | Montants en `BigDecimal`, jamais `float/double` ; verrouillage pessimiste sur les virements ; écritures comptables immuables | `Account`, `Transaction`, `TransactionService` |
| A05 - Security Misconfiguration | En-têtes HSTS/anti-clickjacking/nosniff ; `ddl-auto=validate` (jamais `update`) ; détails d'erreur (message, stacktrace) désactivés au niveau serveur | `SecurityConfig`, `application.yml` |
| A06 - Vulnerable Components | Scan automatisé des dépendances (OWASP Dependency-Check) et de l'image Docker (Trivy) à chaque push | `.github/workflows/ci.yml` |
| A07 - Identification & Auth Failures | Politique de mot de passe renforcée (12+ caractères, complexité) ; limitation des tentatives de connexion ; messages d'erreur d'authentification volontairement génériques et identiques (pas d'énumération de comptes) | `RegisterRequest`, `LoginRateLimiter`, `AuthService` |
| A08 - Software & Data Integrity Failures | Migrations de schéma versionnées et auditées (Flyway) ; pipeline CI qui bloque le merge si les tests échouent | `db/migration/`, `.github/workflows/ci.yml` |
| A09 - Security Logging & Monitoring Failures | Journal d'audit dédié pour toute action sensible, y compris les tentatives refusées, horodaté et non modifiable par le code métier | `AuditAspect`, `AuditLog` |
| A10 - Server-Side Request Forgery | Sans objet dans le périmètre actuel (aucun appel sortant piloté par une entrée utilisateur) | — |

## Principes de conception PCI-DSS appliqués

Ce projet ne traite pas de données de carte bancaire (PAN) et n'est donc pas soumis au PCI-DSS à
proprement parler, mais applique les principes de contrôle transposables à tout système financier :

- **Moindre privilège** : un client ne peut jamais accéder aux ressources d'un autre utilisateur,
  y compris via un identifiant deviné (protection IDOR systématique, pas seulement côté UI).
- **Traçabilité complète** : toute opération sensible est journalisée avec acteur, horodatage,
  adresse IP source et résultat (succès/échec), sans possibilité pour le code métier d'omettre
  cette journalisation (voir `AuditAspect`).
- **Chiffrement des données sensibles** : mots de passe hachés (jamais stockés ni loggés en
  clair) ; le champ mot de passe est explicitement exclu de toute sérialisation JSON et absent
  des messages de log applicatifs.
- **Segmentation réseau** : `docker-compose.yml` n'expose que les ports strictement nécessaires ;
  en production, la base de données ne devrait être joignable que depuis le réseau applicatif
  interne (non exposée publiquement).
- **Gestion des secrets** : aucun secret (mot de passe DB, clé JWT) n'est commité — tous sont
  injectés par variable d'environnement, avec échec explicite au démarrage si absents (voir
  `docker-compose.yml` et `JwtService`). En production réelle, utiliser un coffre-fort dédié
  (HashiCorp Vault, AWS Secrets Manager) plutôt que des variables d'environnement brutes.
- **Rotation et durée de vie limitée des jetons** : jetons JWT à courte durée de vie (30 minutes
  par défaut), sans mécanisme de rafraîchissement automatique implicite.

## Ce qui n'est PAS couvert (hors périmètre assumé de ce projet)

- Authentification multi-facteurs (MFA/TOTP) — voir feuille de route du README.
- Chiffrement applicatif au niveau champ (au-delà du chiffrement au repos fourni par la base).
- Détection d'anomalies / scoring de fraude en temps réel.
- Conformité réglementaire complète (COBAC, PCI-DSS formel) : ce projet illustre une **démarche**
  de sécurité applicable en contexte bancaire, il ne constitue pas une certification.

## Signaler une vulnérabilité

Ce dépôt étant un projet de démonstration, toute remarque de sécurité peut être ouverte
directement en issue GitHub.
