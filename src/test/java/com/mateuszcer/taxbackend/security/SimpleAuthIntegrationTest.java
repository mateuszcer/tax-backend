package com.mateuszcer.taxbackend.security;

import com.mateuszcer.taxbackend.security.infrastructure.User;
import com.mateuszcer.taxbackend.security.infrastructure.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for simple-auth profile.
 * Tests the complete authentication flow without Cognito.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Use test profile for these tests
class SimpleAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSignUpNewUser() throws Exception {
        mockMvc.perform(post("/auth/signUp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "newuser@test.com",
                        "password": "SecurePass123!"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void shouldSignInAndReturnToken() throws Exception {
        // First sign up
        mockMvc.perform(post("/auth/signUp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "signin@test.com",
                        "password": "SecurePass123!"
                    }
                    """));

        // Then sign in
        mockMvc.perform(post("/auth/signIn")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "signin@test.com",
                        "password": "SecurePass123!"
                    }
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.idToken").value(notNullValue()))
                .andExpect(jsonPath("$.data.accessToken").value(notNullValue()));
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        // First sign up
        mockMvc.perform(post("/auth/signUp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "reject@test.com",
                        "password": "CorrectPass123!"
                    }
                    """));

        // Try with wrong password
        mockMvc.perform(post("/auth/signIn")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "reject@test.com",
                        "password": "WrongPassword!"
                    }
                    """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        // First sign up
        mockMvc.perform(post("/auth/signUp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "duplicate@test.com",
                        "password": "Pass123!"
                    }
                    """));

        // Try to sign up again with same email
        mockMvc.perform(post("/auth/signUp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "duplicate@test.com",
                        "password": "DifferentPass123!"
                    }
                    """))
                .andExpect(status().isUnauthorized());
    }
}
