package com.cloudprocessing.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateFileRequest(

    @NotBlank @Size(max = 500)
    String originalName,

    @NotBlank @Size(max = 255)
    String contentType,

    @Positive
    Long sizeBytes
) {}
