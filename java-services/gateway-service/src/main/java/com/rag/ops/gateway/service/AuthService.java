package com.rag.ops.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 认证服务 - 调用 Auth Service 进行认证
 */
@Slf4j
@Service
public class AuthService {

    @Autowired
    private WebClient.Builder webClientBuilder;

    private static final String AUTH_SERVICE_URL = "http://auth-service";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    /**
     * 验证 JWT Token
     */
    public Mono<Boolean> validateToken(String token) {
        log.debug("Validating JWT token");
        
        return webClientBuilder.build()
                .post()
                .uri(AUTH_SERVICE_URL + "/api/auth/validate-token")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(ValidationResponse.class)
                .map(ValidationResponse::isValid)
                .timeout(TIMEOUT)
                .doOnError(e -> log.error("Error calling auth service for token validation", e))
                .onErrorReturn(false);
    }

    /**
     * 验证 API Key
     */
    public Mono<Boolean> validateApiKey(String apiKey) {
        log.debug("Validating API Key");
        
        return webClientBuilder.build()
                .post()
                .uri(AUTH_SERVICE_URL + "/api/auth/validate-api-key")
                .header("X-API-Key", apiKey)
                .retrieve()
                .bodyToMono(ValidationResponse.class)
                .map(ValidationResponse::isValid)
                .timeout(TIMEOUT)
                .doOnError(e -> log.error("Error calling auth service for API key validation", e))
                .onErrorReturn(false);
    }

    /**
     * 验证响应
     */
    private static class ValidationResponse {
        private boolean valid;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }
    }
}
