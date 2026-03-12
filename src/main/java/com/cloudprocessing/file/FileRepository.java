package com.cloudprocessing.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileRecord, UUID> {

    List<FileRecord> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<FileRecord> findByIdAndUserId(UUID id, UUID userId);
}
