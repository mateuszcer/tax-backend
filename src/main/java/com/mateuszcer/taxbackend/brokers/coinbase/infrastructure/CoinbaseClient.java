package com.mateuszcer.taxbackend.brokers.coinbase.infrastructure;

import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.dto.CoinbaseGetOrdersResponse;
import com.mateuszcer.taxbackend.brokers.coinbase.infrastructure.repository.CoinbaseTokenRepository;
import com.mateuszcer.taxbackend.brokers.coinbase.model.CoinbaseToken;
import com.mateuszcer.taxbackend.shared.interceptors.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Service
public class CoinbaseClient {

    private final RestClient restClient;

    private final CoinbaseTokenRepository coinbaseTokenRepository;

    @Value("${coinbase.base-url:https://api.coinbase.com}")
    private String coinbaseBaseUrl;

    @Value("${coinbase.oauth-url:https://login.coinbase.com}")
    private String coinbaseOAuthUrl;

    @Value("${coinbase.oauth-api-url:https://login.coinbase.com}")
    private String coinbaseOAuthApiUrl;

    @Value("${coinbase.client.id}")
    private String clientId;

    @Value("${coinbase.client.secret}")
    private String clientSecret;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;


    @Autowired
    public CoinbaseClient(RestClient.Builder restClientBuilder, CoinbaseTokenRepository coinbaseTokenRepository) {
        this.restClient = restClientBuilder.requestInterceptor(new LoggingInterceptor()).build();
        this.coinbaseTokenRepository = coinbaseTokenRepository;
    }


    public String getRedirectUrl() {
        String redirectUri = frontendBaseUrl;
        
        UriBuilder uriBuilder = UriComponentsBuilder.fromUri(URI.create(coinbaseOAuthUrl + "/oauth2/auth"));
        uriBuilder.queryParam("client_id", clientId);
        uriBuilder.queryParam("response_type", "code");
        uriBuilder.queryParam("scope", "wallet:accounts:read,wallet:transactions:read,offline_access");
        uriBuilder.queryParam("redirect_uri", redirectUri);

        URI uri = uriBuilder.build();
        return uri.toString();
    }

    public TokenResponse getAccessToken(String code) {
        String redirectUri = frontendBaseUrl;

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", redirectUri);

        String tokenUrl = coinbaseOAuthApiUrl + "/oauth2/token";
        TokenResponse tokenResponse = restClient.post().uri(tokenUrl).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formData).retrieve().toEntity(TokenResponse.class).getBody();

        return tokenResponse;
    }

    public TokenResponse refreshAccessToken(String refreshToken) {
        String redirectUri = frontendBaseUrl + "/integrations/callback";
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", redirectUri);
        formData.add("refresh_token", refreshToken);

        String tokenUrl = coinbaseOAuthApiUrl + "/oauth2/token";
        TokenResponse tokenResponse = restClient.post().uri(tokenUrl).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formData).retrieve().toEntity(TokenResponse.class).getBody();

        return tokenResponse;
    }

    public String getUserInfo(String accessToken) {
        String userUrl = coinbaseBaseUrl + "/v2/user/";
        return restClient.get().uri(userUrl).header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).accept(MediaType.APPLICATION_JSON).retrieve().toEntity(String.class).getBody();
    }

    public CoinbaseGetOrdersResponse getOrders(String accessToken) {
        String ordersUrl = coinbaseBaseUrl + "/api/v3/brokerage/orders/historical/batch";
        return restClient.get()
                .uri(ordersUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(CoinbaseGetOrdersResponse.class)
                .getBody();
    }

}
