package com.example.note.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.upload.max-size:5242880}")
    private long maxFileSize;  // 默认5MB

    // 支持的图片格式
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    // 单篇笔记最大图片数
    public static final int MAX_IMAGES_PER_NOTE = 10;

    @Override
    public String uploadImage(MultipartFile file, Long userId) {
        // 1. 验证文件是否为空
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 2. 验证文件大小
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小不能超过" + (maxFileSize / 1024 / 1024) + "MB");
        }

        // 3. 获取文件扩展名并验证
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("不支持该文件格式，仅支持: jpg, png, gif, webp");
        }

        // 4. 创建用户目录
        String userDir = uploadPath + "/" + userId;
        try {
            Files.createDirectories(Paths.get(userDir));
        } catch (IOException e) {
            throw new RuntimeException("创建存储目录失败", e);
        }

        // 5. 生成唯一文件名
        String newFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String filePath = userDir + "/" + newFilename;

        // 6. 保存文件
        try {
            file.transferTo(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败", e);
        }

        // 7. 返回访问URL
        return "/uploads/" + userId + "/" + newFilename;
    }

    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        // 从URL提取文件路径
        if (imageUrl.startsWith("/uploads/")) {
            String filePath = uploadPath + imageUrl.substring("/uploads".length());
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                // 删除失败不抛异常，仅记录日志
                System.err.println("删除图片失败: " + filePath);
            }
        }
    }

    @Override
    public String getStoragePath() {
        return uploadPath;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }
}