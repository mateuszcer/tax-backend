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

    private final String LIST_ORDERS = "https://api.coinbase.com/api/v3/brokerage/orders/historical/batch";
    private final String TOKEN = "https://login.coinbase.com/oauth2/token";

    @Value("${coinbase.client.id}")
    private String clientId;

    @Value("${coinbase.client.secret}")
    private String clientSecret;


    @Autowired
    public CoinbaseClient(RestClient.Builder restClientBuilder, CoinbaseTokenRepository coinbaseTokenRepository) {
        this.restClient = restClientBuilder.requestInterceptor(new LoggingInterceptor()).build();
        this.coinbaseTokenRepository = coinbaseTokenRepository;
    }


    public String getRedirectUrl() {

        UriBuilder uriBuilder = UriComponentsBuilder.fromUri(URI.create("https://login.coinbase.com/oauth2/auth"));
        uriBuilder.queryParam("client_id", clientId);
        uriBuilder.queryParam("response_type", "code");
        uriBuilder.queryParam("scope", "wallet:accounts:read,wallet:transactions:read,offline_access");

        URI uri = uriBuilder.build();
        return uri.toString();
    }

    public TokenResponse getAccessToken(String code) {


        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", "https://www.taxool.com");

        TokenResponse tokenResponse = restClient.post().uri(TOKEN).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formData).retrieve().toEntity(TokenResponse.class).getBody();

        return tokenResponse;
    }

    public TokenResponse refreshAccessToken(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("redirect_uri", "https://www.taxool.com");
        formData.add("refresh_token", refreshToken);

        TokenResponse tokenResponse = restClient.post().uri(TOKEN).contentType(MediaType.APPLICATION_FORM_URLENCODED).body(formData).retrieve().toEntity(TokenResponse.class).getBody();

        return tokenResponse;
    }

    public String getUserInfo(String accessToken) {
        return restClient.get().uri("https://api.coinbase.com/v2/user/").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken).accept(MediaType.APPLICATION_JSON).retrieve().toEntity(String.class).getBody();
    }

    public CoinbaseGetOrdersResponse getOrders(String accessToken) {
        return restClient.get()
                .uri(LIST_ORDERS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toEntity(CoinbaseGetOrdersResponse.class)
                .getBody();
    }

}
