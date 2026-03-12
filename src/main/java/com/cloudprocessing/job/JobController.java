package com.cloudprocessing.job;

import com.cloudprocessing.auth.User;
import com.cloudprocessing.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /** Create a compression job for an uploaded file. */
    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody CreateJobRequest request,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok(jobService.createJob(request, user.getId())));
    }

    /** List all jobs belonging to the authenticated user. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<JobResponse>>> listJobs(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.ok(jobService.listJobs(user.getId())));
    }

    /** Poll a single job for its current status and metrics. */
    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.ok(jobService.getJob(jobId, user.getId())));
    }
}
