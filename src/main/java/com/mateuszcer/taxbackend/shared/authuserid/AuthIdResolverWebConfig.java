package com.mateuszcer.taxbackend.shared.authuserid;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class AuthIdResolverWebConfig implements WebMvcConfigurer {

    private final AuthUserIdResolver authUserIdResolver;

    public AuthIdResolverWebConfig(AuthUserIdResolver authUserIdResolver) {
        this.authUserIdResolver = authUserIdResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserIdResolver);
    }
}
