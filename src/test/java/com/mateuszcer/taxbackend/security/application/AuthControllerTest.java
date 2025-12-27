package com.mateuszcer.taxbackend.security.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mateuszcer.taxbackend.security.CognitoService;
import com.mateuszcer.taxbackend.security.application.dto.UserSignUpRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CognitoService cognitoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void signUp_ValidRequest_ReturnsSuccess() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest("test@example.com", "password123");
        doNothing().when(cognitoService).signUpUser(anyString(), anyString());

        mockMvc.perform(post("/auth/signUp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully. Please check your email for confirmation code."));
    }

    @Test
    @WithMockUser
    void signUp_InvalidEmail_ReturnsBadRequest() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest("invalid-email", "password123");

        mockMvc.perform(post("/auth/signUp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser
    void signUp_ShortPassword_ReturnsBadRequest() throws Exception {
        UserSignUpRequest request = new UserSignUpRequest("test@example.com", "123");

        mockMvc.perform(post("/auth/signUp")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }
}
