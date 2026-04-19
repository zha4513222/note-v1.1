package com.example.note.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    /**
     * 上传图片文件
     * @param file 图片文件
     * @param userId 用户ID
     * @return 图片访问URL
     */
    String uploadImage(MultipartFile file, Long userId);

    /**
     * 删除图片文件
     * @param imageUrl 图片URL
     */
    void deleteImage(String imageUrl);

    /**
     * 获取存储路径
     * @return 存储路径
     */
    String getStoragePath();
}