package com.jtmcloud.securebank.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * La politique de mot de passe (12 caractères min., majuscule, minuscule, chiffre,
 * caractère spécial) reflète les recommandations OWASP ASVS pour les applications
 * financières — volontairement plus stricte que le minimum grand public.
 */
public record RegisterRequest(

        @NotBlank
        @Email
        @Size(max = 150)
        String email,

        @NotBlank
        @Size(min = 12, max = 128)
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
                message = "Le mot de passe doit contenir au moins 12 caractères, une majuscule, "
                        + "une minuscule, un chiffre et un caractère spécial."
        )
        String password,

        @NotBlank
        @Size(min = 2, max = 100)
        String fullName
) {
}
