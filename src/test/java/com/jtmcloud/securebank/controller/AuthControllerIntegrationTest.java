package com.jtmcloud.securebank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration bout-en-bout de la pile d'authentification (contrôleur -> service ->
 * sécurité -> persistance H2), sans dépendance à une base PostgreSQL externe.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql("/schema-test.sql")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerThenLogin_returnsBearerToken() throws Exception {
        Map<String, String> registerBody = Map.of(
                "email", "nouveau.client@securebank.test",
                "password", "Sup3r$ecurePass!",
                "fullName", "Nouveau Client"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        Map<String, String> loginBody = Map.of(
                "email", "nouveau.client@securebank.test",
                "password", "Sup3r$ecurePass!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void register_rejectsWeakPassword() throws Exception {
        Map<String, String> registerBody = Map.of(
                "email", "faible@securebank.test",
                "password", "motdepasse", // ne respecte pas la politique OWASP ASVS
                "fullName", "Test Faible"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerBody)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returnsGenericMessageForUnknownUser() throws Exception {
        Map<String, String> loginBody = Map.of(
                "email", "inexistant@securebank.test",
                "password", "peu-importe-le-mot-de-passe-1234!"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginBody)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }
}
