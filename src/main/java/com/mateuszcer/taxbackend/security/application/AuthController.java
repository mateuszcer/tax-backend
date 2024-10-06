package com.mateuszcer.taxbackend.security.application;

import com.mateuszcer.taxbackend.security.CognitoService;
import com.mateuszcer.taxbackend.security.application.dto.UserConfirmSignUpRequest;
import com.mateuszcer.taxbackend.security.application.dto.UserSignInRequest;
import com.mateuszcer.taxbackend.security.application.dto.UserSignUpRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final CognitoService cognitoService;

    public AuthController(CognitoService cognitoService) {
        this.cognitoService = cognitoService;
    }

    @PostMapping("/signUp")
    public void signUp(@RequestBody UserSignUpRequest userSignUpRequest) {
        cognitoService.signUpUser(userSignUpRequest.email(), userSignUpRequest.password());
    }

    @PostMapping("/confirm")
    public void confirmSignUp(@RequestBody UserConfirmSignUpRequest userConfirmSignUpRequest) {
        cognitoService.confirmSignUp(userConfirmSignUpRequest.email(), userConfirmSignUpRequest.code());
    }

    @PostMapping("/signIn")
    public String login(@RequestBody UserSignInRequest userSignInRequest) {
        return cognitoService.signIn(userSignInRequest.email(), userSignInRequest.password()).idToken();
    }
}
