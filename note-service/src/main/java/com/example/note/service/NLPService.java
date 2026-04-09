package com.example.note.service;

import com.example.note.dto.KeywordVO;
import java.util.List;

public interface NLPService {

    /**
     * 从文本内容提取关键词
     * @param content 文本内容
     * @param topN 返回的关键词数量
     * @return 关键词列表及其权重
     */
    List<KeywordVO> extractKeywords(String content, int topN);
}
