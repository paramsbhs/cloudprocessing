package com.cloudprocessing.file;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<FileRecord, UUID> {

    List<FileRecord> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<FileRecord> findByIdAndUserId(UUID id, UUID userId);

    @Query(value = "SELECT COUNT(*) FROM files WHERE user_id = :userId", nativeQuery = true)
    long countByUserId(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE(SUM(size_bytes - output_size_bytes), 0)
        FROM files
        WHERE user_id = :userId AND status = 'COMPLETED'
          AND output_size_bytes IS NOT NULL AND size_bytes IS NOT NULL
        """, nativeQuery = true)
    long totalBytesSaved(@Param("userId") UUID userId);

    @Query(value = """
        SELECT COALESCE(AVG(1.0 - CAST(output_size_bytes AS float) / NULLIF(size_bytes, 0)), 0)
        FROM files
        WHERE user_id = :userId AND status = 'COMPLETED'
          AND output_size_bytes IS NOT NULL AND size_bytes > 0
        """, nativeQuery = true)
    double avgCompressionRatio(@Param("userId") UUID userId);
}
