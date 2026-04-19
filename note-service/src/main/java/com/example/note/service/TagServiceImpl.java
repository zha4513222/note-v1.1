package com.example.note.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.note.dto.KeywordVO;
import com.example.note.dto.TagVO;
import com.example.note.entity.Tag;
import com.example.note.entity.UserTagHistory;
import com.example.note.repository.TagMapper;
import com.example.note.repository.UserTagHistoryMapper;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final UserTagHistoryMapper userTagHistoryMapper;
    private final NLPService nlpService;
    private final Cache<String, Object> recommendationCache;
    private final Cache<Long, Object> userHistoryCache;

    // 评分权重（可通过配置文件覆盖）
    @Value("${tag.recommend.keyword-match-score:30}")
    private int keywordMatchScore;

    @Value("${tag.recommend.user-history-score:25}")
    private int userHistoryScore;

    @Value("${tag.recommend.hot-tag-score:10}")
    private int hotTagScore;

    @Value("${tag.recommend.recency-score:5}")
    private int recencyScore;

    @Value("${tag.recommend.max-results:5}")
    private int maxResults;

    public TagServiceImpl(TagMapper tagMapper, UserTagHistoryMapper userTagHistoryMapper,
                         NLPService nlpService,
                         Cache<String, Object> tagRecommendationCache,
                         Cache<Long, Object> userTagHistoryCache) {
        this.tagMapper = tagMapper;
        this.userTagHistoryMapper = userTagHistoryMapper;
        this.nlpService = nlpService;
        this.recommendationCache = tagRecommendationCache;
        this.userHistoryCache = userTagHistoryCache;
    }

    @Override
    public List<TagVO> getUserTags(Long userId) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getUseCount);
        return tagMapper.selectList(wrapper).stream()
            .map(this::toTagVO)
            .collect(Collectors.toList());
    }

    @Override
    public List<TagVO> getPopularTags(int limit) {
        return tagMapper.selectPopular(limit).stream()
            .map(this::toTagVO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TagVO createTag(String name) {
        Tag tag = new Tag();
        tag.setName(name);
        tag.setUseCount(0);
        tagMapper.insert(tag);
        return toTagVO(tag);
    }

    @Override
    public void deleteTag(Long id) {
        tagMapper.deleteById(id);
    }

    @Override
    public List<TagVO> recommendTags(Long userId, String content, List<Long> existingTagIds) {
        // 生成缓存key
        String cacheKey = generateCacheKey(userId, content, existingTagIds);

        // 尝试从缓存获取
        @SuppressWarnings("unchecked")
        List<TagVO> cachedResult = (List<TagVO>) recommendationCache.getIfPresent(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // 执行推荐算法
        List<TagVO> result = computeRecommendations(userId, content, existingTagIds);

        // 存入缓存
        recommendationCache.put(cacheKey, result);

        return result;
    }

    private List<TagVO> computeRecommendations(Long userId, String content, List<Long> existingTagIds) {
        // 1. 从内容提取关键词（最多10个，包含权重信息）
        List<KeywordVO> keywords = extractKeywords(content);

        // 2. 获取用户历史标签数据
        UserTagData userTagData = getUserTagData(userId);

        // 3. 获取热门标签
        Set<Long> hotTagIds = tagMapper.selectPopular(10).stream()
            .map(Tag::getId)
            .collect(Collectors.toSet());

        // 4. 获取所有可用标签
        List<Tag> allTags = getAvailableTags(existingTagIds);

        // 5. 计算每个标签的推荐得分
        return calculateTagScores(allTags, keywords, userTagData, hotTagIds);
    }

    private List<KeywordVO> extractKeywords(String content) {
        List<KeywordVO> keywords = new ArrayList<>();
        Set<String> keywordSet = new HashSet<>();

        if (content != null && !content.isEmpty()) {
            List<KeywordVO> extracted = nlpService.extractKeywords(content, 10);
            for (KeywordVO kw : extracted) {
                String word = kw.getWord().toLowerCase();
                if (!keywordSet.contains(word) && word.length() > 1) {
                    keywords.add(kw);
                    keywordSet.add(word);
                }
            }
        }
        return keywords;
    }

    private UserTagData getUserTagData(Long userId) {
        UserTagData data = new UserTagData();

        if (userId != null) {
            // 尝试从缓存获取
            @SuppressWarnings("unchecked")
            UserTagData cached = (UserTagData) userHistoryCache.getIfPresent(userId);
            if (cached != null) {
                return cached;
            }

            LambdaQueryWrapper<UserTagHistory> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserTagHistory::getUserId, userId);
            wrapper.orderByDesc(UserTagHistory::getLastUsedAt);
            List<UserTagHistory> history = userTagHistoryMapper.selectList(wrapper);

            for (UserTagHistory h : history) {
                data.usageCount.put(h.getTagId(), h.getUseCount());
                data.recency.put(h.getTagId(), h.getLastUsedAt());
            }

            // 存入缓存
            userHistoryCache.put(userId, data);
        }

        return data;
    }

    private List<Tag> getAvailableTags(List<Long> existingTagIds) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Tag::getUseCount);
        if (existingTagIds != null && !existingTagIds.isEmpty()) {
            wrapper.notIn(Tag::getId, existingTagIds);
        }
        return tagMapper.selectList(wrapper);
    }

    private List<TagVO> calculateTagScores(List<Tag> allTags, List<KeywordVO> keywords,
                                           UserTagData userTagData, Set<Long> hotTagIds) {
        List<TagScore> scores = new ArrayList<>();

        for (Tag tag : allTags) {
            double score = 0;
            String tagName = tag.getName().toLowerCase();

            // 关键词匹配得分（使用关键词权重）
            score += calculateKeywordScore(tagName, keywords);

            // 用户历史使用得分
            score += calculateUserHistoryScore(tag.getId(), tagName, userTagData);

            // 热门标签得分
            if (hotTagIds.contains(tag.getId())) {
                score += hotTagScore;
            }

            // 标签使用次数归一化
            score += Math.log(tag.getUseCount() + 1) * 2;

            if (score > 0) {
                scores.add(new TagScore(tag, score));
            }
        }

        return scores.stream()
            .sorted((a, b) -> Double.compare(b.score, a.score))
            .limit(maxResults)
            .map(ts -> toTagVO(ts.tag))
            .collect(Collectors.toList());
    }

    private double calculateKeywordScore(String tagName, List<KeywordVO> keywords) {
        double score = 0;
        for (KeywordVO kw : keywords) {
            String word = kw.getWord().toLowerCase();
            float weight = kw.getWeight() != null ? kw.getWeight() : 0.1f;

            if (tagName.equals(word)) {
                // 完全匹配：使用关键词权重放大
                score += keywordMatchScore * (1 + weight * 10);
                break;
            } else if (tagName.contains(word) || word.contains(tagName)) {
                // 部分匹配：使用一半权重
                score += keywordMatchScore * 0.5 * (1 + weight * 10);
                break;
            }
        }
        return score;
    }

    private double calculateUserHistoryScore(Long tagId, String tagName, UserTagData userTagData) {
        double score = 0;
        Integer usageCount = userTagData.usageCount.get(tagId);

        if (usageCount != null) {
            score += Math.min(userHistoryScore, usageCount * 3);

            LocalDateTime lastUsed = userTagData.recency.get(tagId);
            if (lastUsed != null) {
                long daysSinceUsed = java.time.temporal.ChronoUnit.DAYS.between(lastUsed, LocalDateTime.now());
                if (daysSinceUsed < 7) {
                    score += recencyScore;
                } else if (daysSinceUsed < 30) {
                    score += recencyScore * 0.5;
                }
            }
        }
        return score;
    }

    private String generateCacheKey(Long userId, String content, List<Long> existingTagIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("recommend:").append(userId != null ? userId : "0");
        if (content != null) {
            sb.append(":").append(content.hashCode());
        }
        if (existingTagIds != null && !existingTagIds.isEmpty()) {
            sb.append(":").append(existingTagIds.stream().sorted().map(String::valueOf).collect(Collectors.joining(",")));
        }
        return sb.toString();
    }

    public void invalidateUserCache(Long userId) {
        if (userId != null) {
            userHistoryCache.invalidate(userId);
        }
    }

    public void invalidateRecommendationCache() {
        recommendationCache.invalidateAll();
    }

    @Override
    public List<TagVO> generateTagsFromContent(String content, int maxTags) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 从内容提取关键词
        List<KeywordVO> keywords = nlpService.extractKeywords(content, 10);

        // 选择权重最高的关键词作为标签候选（最多maxTags个）
        List<TagVO> generatedTags = new ArrayList<>();
        Set<String> createdNames = new HashSet<>();

        for (KeywordVO kw : keywords) {
            String word = kw.getWord();
            if (word == null || word.length() < 2 || createdNames.contains(word.toLowerCase())) {
                continue;
            }

            // 检查是否已存在同名标签
            LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Tag::getName, word);
            Tag existingTag = tagMapper.selectOne(wrapper);

            if (existingTag != null) {
                // 标签已存在，直接使用
                generatedTags.add(toTagVO(existingTag));
            } else {
                // 创建新标签
                TagVO newTag = createTag(word);
                generatedTags.add(newTag);
            }

            createdNames.add(word.toLowerCase());

            if (generatedTags.size() >= maxTags) {
                break;
            }
        }

        return generatedTags;
    }

    private TagVO toTagVO(Tag tag) {
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getName());
        vo.setUseCount(tag.getUseCount());
        return vo;
    }

    private static class TagScore {
        Tag tag;
        double score;

        TagScore(Tag tag, double score) {
            this.tag = tag;
            this.score = score;
        }
    }

    private static class UserTagData {
        Map<Long, Integer> usageCount = new HashMap<>();
        Map<Long, LocalDateTime> recency = new HashMap<>();
    }
}
