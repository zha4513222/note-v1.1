package com.example.note.service;

import com.example.note.dto.TagVO;
import java.util.List;

public interface TagService {

    List<TagVO> getUserTags(Long userId);

    List<TagVO> getPopularTags(int limit);

    TagVO createTag(String name);

    void deleteTag(Long id);

    List<TagVO> recommendTags(Long userId, String content, List<Long> existingTagIds);

    void invalidateUserCache(Long userId);

    void invalidateRecommendationCache();
}
