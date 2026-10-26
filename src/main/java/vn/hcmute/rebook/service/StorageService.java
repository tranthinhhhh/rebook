package vn.hcmute.rebook.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /** Upload ảnh/video, trả về URL truy cập (Cloudinary hoặc /uploads/...) */
    String upload(MultipartFile file, String folder);

    boolean isCloudinaryEnabled();
}
