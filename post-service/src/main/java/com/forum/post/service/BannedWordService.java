package com.forum.post.service;

import com.forum.common.constant.ForumConstants;
import com.forum.common.exception.BusinessException;
import com.forum.common.util.ContentNormalizer;
import com.forum.post.entity.BannedWord;
import com.forum.post.mapper.BannedWordMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BannedWordService {

    private final BannedWordMapper bannedWordMapper;
    private volatile List<String> cachedWords = List.of();

    @PostConstruct
    public void init() {
        refreshCache();
    }

    public List<BannedWord> listAll() {
        return bannedWordMapper.listAll();
    }

    public BannedWord addWord(String word) {
        if (word == null || word.isBlank()) {
            throw new BusinessException("违禁词不能为空");
        }
        String normalized = word.trim();
        if (normalized.length() > 100) {
            throw new BusinessException("违禁词过长");
        }
        if (bannedWordMapper.countByWord(normalized) > 0) {
            throw new BusinessException("该违禁词已存在");
        }
        BannedWord bannedWord = new BannedWord();
        bannedWord.setWord(normalized);
        bannedWordMapper.insert(bannedWord);
        refreshCache();
        return bannedWord;
    }

    public void deleteWord(Long id) {
        bannedWordMapper.delete(id);
        refreshCache();
    }

    public void assertClean(String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        String normalized = ContentNormalizer.forSensitiveCheck(content);
        if (normalized.isEmpty()) {
            return;
        }
        for (String word : cachedWords) {
            if (!word.isEmpty() && normalized.contains(word)) {
                throw new BusinessException(ForumConstants.CODE_SENSITIVE_CONTENT, ForumConstants.MSG_SENSITIVE_CONTENT);
            }
        }
    }

    public synchronized void refreshCache() {
        List<String> words = new ArrayList<>();
        for (BannedWord bannedWord : bannedWordMapper.listEnabled()) {
            String normalized = ContentNormalizer.forSensitiveCheck(bannedWord.getWord());
            if (!normalized.isEmpty()) {
                words.add(normalized);
            }
        }
        cachedWords = List.copyOf(words);
    }
}
