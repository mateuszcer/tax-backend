package com.mateuszcer.taxbackend.security.application.dto;

public record UserConfirmSignUpRequest(String email, String code) {}