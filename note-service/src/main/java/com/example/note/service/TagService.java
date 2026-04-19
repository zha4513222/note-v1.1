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

    /**
     * 从内容提取关键词并生成新标签
     * @param content 笔记内容
     * @param maxTags 最大标签数量（默认3）
     * @return 生成的标签列表
     */
    List<TagVO> generateTagsFromContent(String content, int maxTags);
}
