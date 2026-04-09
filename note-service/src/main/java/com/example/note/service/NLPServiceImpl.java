package com.example.note.service;

import com.example.note.dto.KeywordVO;
import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.huaban.analysis.jieba.Word;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class NLPServiceImpl implements NLPService {

    // Jieba 分词器（线程安全，整个应用共享）
    private static final JiebaSegmenter JIEBA_SEGMENTER = new JiebaSegmenter();

    // 中文停用词表
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
        "的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
        "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
        "自己", "这", "那", "什么", "他", "她", "它", "们", "这个", "那个", "为", "与",
        "或", "而", "及", "等", "于", "被", "把", "让", "从", "向", "对", "所",
        "如果", "因为", "所以", "但是", "虽然", "然后", "而且", "并且", "或者", "已经",
        "可以", "可能", "应该", "还有", "知道", "发现", "开始", "提供", "认为", "成为",
        "这里", "这么", "如何", "为什么", "只是", "必须", "只能", "需要", "进行", "使用",
        "啊", "吧", "呢", "哦", "嗯", "呀", "哇", "哈", "噢", "唉", "诶", "嘿"
    ));

    @Override
    public List<KeywordVO> extractKeywords(String content, int topN) {
        if (content == null || content.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 使用 Jieba 分词
        List<SegToken> tokens = JIEBA_SEGMENTER.process(content, JiebaSegmenter.SegMode.INDEX);

        // 词频统计（只保留长度>=2的非停用词）
        Map<String, Integer> wordCount = new HashMap<>();
        for (SegToken token : tokens) {
            // SegToken.word 是一个 Word 对象，需要转换为字符串
            String word = token.word.toString();
            // 过滤停用词、纯数字、太短的词
            if (word == null || word.trim().isEmpty()) {
                continue;
            }
            word = word.trim();
            if (word.length() >= 2 && !STOP_WORDS.contains(word) && !isNumeric(word)) {
                wordCount.merge(word, 1, Integer::sum);
            }
        }

        // 计算归一化权重（TF）
        int totalWords = wordCount.values().stream().mapToInt(Integer::intValue).sum();
        if (totalWords == 0) {
            return Collections.emptyList();
        }

        return wordCount.entrySet().stream()
            .map(entry -> {
                KeywordVO kv = new KeywordVO();
                kv.setWord(entry.getKey());
                // 归一化权重：词频 / 总词数
                kv.setWeight((float) entry.getValue() / totalWords);
                return kv;
            })
            .sorted((a, b) -> Float.compare(b.getWeight(), a.getWeight()))
            .limit(topN)
            .collect(Collectors.toList());
    }

    private boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
