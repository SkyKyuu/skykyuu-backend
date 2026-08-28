package com.skykyuu.backend.system.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final HealthResponse HEALTHY_RESPONSE =
            new HealthResponse("skykyuu-backend", "UP");

    @GetMapping
    public Mono<HealthResponse> health() {
        return Mono.just(HEALTHY_RESPONSE);
    }
}
