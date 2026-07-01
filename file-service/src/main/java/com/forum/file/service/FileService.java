package com.forum.file.service;

import com.forum.common.exception.BusinessException;
import com.forum.file.entity.FileMeta;
import com.forum.file.mapper.FileMetaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> MEDIA_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm", "video/quicktime", "video/x-msvideo"
    );

    private static final Set<String> ATTACHMENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip",
            "application/x-zip-compressed",
            "text/plain",
            "text/csv",
            "application/json"
    );

    private static final Map<String, String> EXTENSION_MIME = Map.ofEntries(
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".webm", "video/webm"),
            Map.entry(".mov", "video/quicktime"),
            Map.entry(".avi", "video/x-msvideo"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry(".xls", "application/vnd.ms-excel"),
            Map.entry(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry(".ppt", "application/vnd.ms-powerpoint"),
            Map.entry(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry(".zip", "application/zip"),
            Map.entry(".txt", "text/plain"),
            Map.entry(".csv", "text/csv"),
            Map.entry(".json", "application/json")
    );

    private static final long MEDIA_MAX_SIZE = 50 * 1024 * 1024L;
    private static final long ATTACHMENT_MAX_SIZE = 20 * 1024 * 1024L;

    private final FileMetaMapper fileMetaMapper;

    @Value("${forum.upload-dir:uploads}")
    private String uploadDir;

    @Value("${forum.public-base-url:/api/files/uploads}")
    private String publicBaseUrl;

    public FileMeta upload(MultipartFile file, Long uploaderId, String category) throws IOException {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        String normalizedCategory = normalizeCategory(category);
        long maxSize = "media".equals(normalizedCategory) ? MEDIA_MAX_SIZE : ATTACHMENT_MAX_SIZE;
        if (file.getSize() > maxSize) {
            throw new BusinessException("media".equals(normalizedCategory)
                    ? "图片/视频大小不能超过 50MB"
                    : "文件大小不能超过 20MB");
        }
        String mime = resolveMime(file);
        Set<String> allowed = "media".equals(normalizedCategory) ? MEDIA_TYPES : ATTACHMENT_TYPES;
        if (!allowed.contains(mime)) {
            throw new BusinessException("media".equals(normalizedCategory)
                    ? "仅支持 JPG/PNG/GIF/WEBP 图片及 MP4/WEBM/MOV/AVI 视频"
                    : "不支持该文件类型，请上传 PDF/Office/TXT/ZIP 等常见文档");
        }
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String ext = originalExtension(file.getOriginalFilename(), mime);
        String storedName = UUID.randomUUID() + ext;
        Path target = dir.resolve(storedName);
        Files.copy(file.getInputStream(), target);
        FileMeta meta = new FileMeta();
        meta.setOriginalName(file.getOriginalFilename());
        meta.setStoredName(storedName);
        meta.setUrl(publicBaseUrl + "/" + storedName);
        meta.setUploaderId(uploaderId);
        meta.setSize(file.getSize());
        meta.setMime(mime);
        fileMetaMapper.insert(meta);
        return meta;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "media";
        }
        String value = category.trim().toLowerCase(Locale.ROOT);
        if (!"media".equals(value) && !"attachment".equals(value)) {
            throw new BusinessException("上传类型无效");
        }
        return value;
    }

    private String resolveMime(MultipartFile file) {
        String mime = file.getContentType();
        if (mime != null && !mime.isBlank() && !"application/octet-stream".equalsIgnoreCase(mime)) {
            return mime.toLowerCase(Locale.ROOT);
        }
        String ext = originalExtension(file.getOriginalFilename(), null);
        return EXTENSION_MIME.getOrDefault(ext.toLowerCase(Locale.ROOT), "application/octet-stream");
    }

    private String originalExtension(String name, String mime) {
        if (name != null && name.contains(".")) {
            return name.substring(name.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        }
        if (mime != null) {
            return switch (mime) {
                case "image/jpeg" -> ".jpg";
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                case "image/webp" -> ".webp";
                case "video/mp4" -> ".mp4";
                case "video/webm" -> ".webm";
                case "video/quicktime" -> ".mov";
                case "video/x-msvideo" -> ".avi";
                case "application/pdf" -> ".pdf";
                case "text/plain" -> ".txt";
                default -> ".bin";
            };
        }
        return ".bin";
    }
}
