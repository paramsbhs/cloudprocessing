package com.cloudprocessing.stats;

import com.cloudprocessing.auth.User;
import com.cloudprocessing.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stats")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<StatsResponse>> getStats(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.ok(statsService.getStats(user.getId())));
    }
}
