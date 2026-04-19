package com.example.note.controller;

import com.example.note.common.Result;
import com.example.note.service.FileUploadService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * 上传图片
     */
    @PostMapping("/upload/image")
    public Result<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        try {
            String url = fileUploadService.uploadImage(file, userId);
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            return Result.success(data);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("图片上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除图片
     */
    @DeleteMapping("/image")
    public Result<Void> deleteImage(@RequestParam String url) {
        try {
            fileUploadService.deleteImage(url);
            return Result.success();
        } catch (Exception e) {
            return Result.error("删除图片失败");
        }
    }
}