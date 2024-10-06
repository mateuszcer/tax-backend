package com.mateuszcer.taxbackend.security;

import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
public class CognitoService {

    @Value("${cognito.userPoolId}")
    private String userPoolId;

    @Value("${cognito.clientId}")
    private String clientId;

    @Value("${cognito.clientSecret}")
    private String clientSecret;

    private final CognitoIdentityProviderClient cognitoClient;

    public CognitoService(CognitoIdentityProviderClient cognitoClient) {
        this.cognitoClient = cognitoClient;
    }

    private String calculateSecretHash(String username) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(keySpec);

            String message = username + clientId;
            byte[] hash = hmac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Unable to calculate secret hash", e);
        }
    }

    public void signUpUser(String email, String password) {
        SignUpRequest signUpRequest = SignUpRequest.builder()
                .clientId(clientId)
                .username(email)
                .password(password)
                .userAttributes(AttributeType.builder()
                        .name("email")
                        .value(email)
                        .build())
                .secretHash(calculateSecretHash(email))
                .build();

        cognitoClient.signUp(signUpRequest);
    }

    public void confirmSignUp(String email, String confirmationCode) {
        ConfirmSignUpRequest confirmSignUpRequest = ConfirmSignUpRequest.builder()
                .clientId(clientId)
                .username(email)
                .confirmationCode(confirmationCode)
                .secretHash(calculateSecretHash(email))
                .build();

        cognitoClient.confirmSignUp(confirmSignUpRequest);
    }

    public AuthenticationResultType signIn(String email, String password) {
        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .userPoolId(userPoolId)
                .clientId(clientId)
                .authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
                .authParameters(Map.of("USERNAME", email, "PASSWORD", password,
                        "SECRET_HASH", calculateSecretHash(email)))
                .build();

        AdminInitiateAuthResponse authResponse = cognitoClient.adminInitiateAuth(authRequest);
        return authResponse.authenticationResult();
    }
}
