package com.cloudprocessing.file;

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
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /** Create a file record and receive a presigned S3 upload URL. */
    @PostMapping
    public ResponseEntity<ApiResponse<FileResponse>> createFile(
            @Valid @RequestBody CreateFileRequest request,
            @AuthenticationPrincipal User user) {

        FileResponse response = fileService.createFile(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    /** List all files belonging to the authenticated user. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileResponse>>> listFiles(
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.ok(fileService.listFiles(user.getId())));
    }

    /** Get metadata for a single file. */
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileResponse>> getFile(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.ok(fileService.getFile(fileId, user.getId())));
    }

    /** Confirm that the client has finished uploading to S3. */
    @PostMapping("/{fileId}/confirm-upload")
    public ResponseEntity<ApiResponse<FileResponse>> confirmUpload(
            @PathVariable UUID fileId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(ApiResponse.ok(fileService.confirmUpload(fileId, user.getId())));
    }
}
