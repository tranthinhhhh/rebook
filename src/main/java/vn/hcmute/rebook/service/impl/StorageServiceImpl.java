package vn.hcmute.rebook.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.hcmute.rebook.exception.BusinessException;
import vn.hcmute.rebook.service.StorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class StorageServiceImpl implements StorageService {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp", "mp4", "webm", "mov");

    private final Cloudinary cloudinary;
    private final Path uploadRoot;

    public StorageServiceImpl(@Value("${app.cloudinary.cloud-name}") String cloudName,
                              @Value("${app.cloudinary.api-key}") String apiKey,
                              @Value("${app.cloudinary.api-secret}") String apiSecret,
                              @Value("${app.upload-dir}") String uploadDir) {
        this.cloudinary = cloudName.isBlank() ? null : new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName, "api_key", apiKey, "api_secret", apiSecret, "secure", true));
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath();
    }

    @Override
    public boolean isCloudinaryEnabled() {
        return cloudinary != null;
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) return null;
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
        if (!ALLOWED.contains(ext)) {
            throw new BusinessException("Chỉ chấp nhận ảnh (jpg, png, gif, webp) hoặc video (mp4, webm, mov)");
        }
        try {
            if (cloudinary != null) {
                Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                        ObjectUtils.asMap("folder", "rebook/" + folder, "resource_type", "auto"));
                return (String) result.get("secure_url");
            }
            Path dir = uploadRoot.resolve(folder);
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "." + ext;
            Files.copy(file.getInputStream(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + folder + "/" + name;
        } catch (IOException e) {
            log.error("Upload thất bại", e);
            throw new BusinessException("Upload file thất bại, vui lòng thử lại");
        }
    }
}
