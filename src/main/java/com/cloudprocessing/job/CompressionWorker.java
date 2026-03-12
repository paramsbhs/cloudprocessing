package com.cloudprocessing.job;

import com.cloudprocessing.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class CompressionWorker {

    private static final Logger log = LoggerFactory.getLogger(CompressionWorker.class);
    private static final float JPEG_QUALITY = 0.50f;

    private final JobService jobService;
    private final StorageService storageService;

    public CompressionWorker(JobService jobService, StorageService storageService) {
        this.jobService = jobService;
        this.storageService = storageService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("jobExecutor")
    public void onJobCreated(JobCreatedEvent event) {
        log.debug("Received JobCreatedEvent jobId={}", event.jobId());
        processJob(event.jobId(), event.storageKey(), event.contentType(),
            event.originalName(), event.compressionFormat());
    }

    public void processJob(UUID jobId, String storageKey, String contentType,
                           String originalName, CompressionFormat format) {
        log.info("Processing jobId={} format={} storageKey={}", jobId, format, storageKey);

        if (!jobService.markProcessing(jobId)) {
            log.info("Job {} already claimed by another worker, skipping", jobId);
            return;
        }

        try {
            byte[] input = storageService.downloadObject(storageKey);

            // Images are already DEFLATE-compressed — GZIP/ZIP only adds overhead; re-encode as JPEG instead.
            boolean isImageContent = contentType != null && contentType.startsWith("image/");
            if (isImageContent && (format == CompressionFormat.GZIP || format == CompressionFormat.ZIP)) {
                log.info("Job {} — image type '{}' with {}; promoting to JPEG re-encode", jobId, contentType, format);
                format = CompressionFormat.JPEG;
            }

            // PDFs already use internal zlib compression — use PDFBox optimizer and output a smaller PDF.
            boolean isPdfContent = "application/pdf".equals(contentType);
            if (isPdfContent && (format == CompressionFormat.GZIP || format == CompressionFormat.ZIP)) {
                log.info("Job {} — PDF with {}; applying PDFBox optimization", jobId, format);
                byte[] output = compressPdf(input);
                int lastDot = storageKey.lastIndexOf('.');
                String base = (lastDot > 0) ? storageKey.substring(0, lastDot) : storageKey;
                String outputKey = base + "-compressed.pdf";
                long outputSize = storageService.uploadObject(outputKey, output, "application/pdf");
                jobService.markCompleted(jobId, outputKey, outputSize);
                log.info("Job {} complete (PDF): {} → {} bytes (key={})", jobId, input.length, outputSize, outputKey);
                return;
            }

            byte[] output;
            String outputContentType;

            switch (format) {
                case JPEG -> {
                    output = compressImage(input);
                    outputContentType = "image/jpeg";
                }
                case ZIP -> {
                    output = zipCompress(input, originalName);
                    outputContentType = "application/zip";
                }
                default -> {   // GZIP
                    output = gzipCompress(input);
                    outputContentType = "application/gzip";
                }
            }

            String outputKey = buildOutputKey(storageKey, format);
            long outputSize = storageService.uploadObject(outputKey, output, outputContentType);

            jobService.markCompleted(jobId, outputKey, outputSize);
            log.info("Job {} complete: {} → {} bytes (key={})", jobId, input.length, outputSize, outputKey);

        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage(), e);
            jobService.markFailedOrRetry(jobId, e.getMessage());
        }
    }

    // ── Compression helpers ───────────────────────────────────────────────

    private byte[] compressImage(byte[] input) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(input));
        if (image == null) {
            throw new IOException("Could not decode image data");
        }
        // Flatten alpha channel — JPEG does not support transparency
        if (image.getColorModel().hasAlpha()) {
            BufferedImage rgb = new BufferedImage(
                image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(image, 0, 0, null);
            g.dispose();
            image = rgb;
        }
        var writers = ImageIO.getImageWritersByMIMEType("image/jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG ImageWriter available on this JVM");
        }
        ImageWriter writer = writers.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(JPEG_QUALITY);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.setOutput(ImageIO.createImageOutputStream(out));
        writer.write(null, new IIOImage(image, null, null), params);
        writer.dispose();
        return out.toByteArray();
    }

    private byte[] gzipCompress(byte[] input) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Level 9 = BEST_COMPRESSION (default is 6)
        GZIPOutputStream gz = new GZIPOutputStream(out) {{ def.setLevel(Deflater.BEST_COMPRESSION); }};
        try (gz) { gz.write(input); }
        return out.toByteArray();
    }

    private byte[] zipCompress(byte[] input, String entryName) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.setLevel(Deflater.BEST_COMPRESSION);
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(input);
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    /**
     * Compresses a PDF using Ghostscript's /ebook preset (150 dpi image downsampling,
     * font subsetting, stream compression). Typically achieves 30-70% reduction on
     * image-heavy PDFs and 10-30% on text-heavy ones.
     */
    private byte[] compressPdf(byte[] input) throws IOException {
        Path inFile  = Files.createTempFile("cp-pdf-in-",  ".pdf");
        Path outFile = Files.createTempFile("cp-pdf-out-", ".pdf");
        try {
            Files.write(inFile, input);
            ProcessBuilder pb = new ProcessBuilder(
                "gs",
                "-sDEVICE=pdfwrite",
                "-dCompatibilityLevel=1.4",
                "-dPDFSETTINGS=/ebook",   // 150 dpi — good quality/size balance
                "-dNOPAUSE", "-dQUIET", "-dBATCH",
                "-sOutputFile=" + outFile.toAbsolutePath(),
                inFile.toAbsolutePath().toString()
            );
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process proc = pb.start();
            boolean finished = proc.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new IOException("Ghostscript timed out after 120s");
            }
            if (proc.exitValue() != 0) {
                throw new IOException("Ghostscript exited with code " + proc.exitValue());
            }
            return Files.readAllBytes(outFile);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("PDF compression interrupted", ie);
        } finally {
            Files.deleteIfExists(inFile);
            Files.deleteIfExists(outFile);
        }
    }

    private String buildOutputKey(String originalKey, CompressionFormat format) {
        String ext = switch (format) {
            case ZIP  -> ".zip";
            case JPEG -> ".jpg";
            default   -> ".gz";
        };
        int lastDot = originalKey.lastIndexOf('.');
        String base = (lastDot > 0) ? originalKey.substring(0, lastDot) : originalKey;
        return base + "-compressed" + ext;
    }
}
