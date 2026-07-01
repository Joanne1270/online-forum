package com.forum.post.service;

import com.forum.post.dto.TagVO;
import com.forum.post.util.PostContentNormalizer;
import com.forum.post.entity.Tag;
import com.forum.post.mapper.PostTagMapper;
import com.forum.post.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagService {

    private static final Pattern HASHTAG_PATTERN = PostContentNormalizer.HASHTAG_PATTERN;
    private static final int MAX_TAGS_PER_POST = 20;

    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;

    public List<TagVO> searchTags(String keyword, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 30);
        String normalized = normalizeTagName(keyword);
        List<Tag> tags = normalized.isEmpty()
                ? tagMapper.listPopular(safeLimit)
                : tagMapper.search(normalized, safeLimit);
        return tags.stream().map(TagVO::from).collect(Collectors.toList());
    }

    public List<String> listTagNamesByPostId(Long postId) {
        if (postId == null) {
            return Collections.emptyList();
        }
        return postTagMapper.listTagNamesByPostId(postId);
    }

    @Transactional
    public void syncPostTags(Long postId, String content) {
        if (postId == null) {
            return;
        }
        List<Long> oldTagIds = postTagMapper.listTagIdsByPostId(postId);
        postTagMapper.deleteByPostId(postId);
        for (Long tagId : oldTagIds) {
            tagMapper.updateUsageCount(tagId, -1);
        }

        LinkedHashSet<String> tagNames = extractTagNames(content);
        int count = 0;
        for (String name : tagNames) {
            if (count >= MAX_TAGS_PER_POST) {
                break;
            }
            Tag tag = tagMapper.findByName(name);
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tagMapper.insert(tag);
            }
            if (postTagMapper.insert(postId, tag.getId()) > 0) {
                tagMapper.updateUsageCount(tag.getId(), 1);
                count++;
            }
        }
    }

    @Transactional
    public void removePostTags(Long postId) {
        if (postId == null) {
            return;
        }
        List<Long> tagIds = postTagMapper.listTagIdsByPostId(postId);
        postTagMapper.deleteByPostId(postId);
        for (Long tagId : tagIds) {
            tagMapper.updateUsageCount(tagId, -1);
        }
    }

    public String normalizeTagName(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > 50) {
            return "";
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    public LinkedHashSet<String> extractTagNames(String content) {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return names;
        }
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            String normalized = normalizeTagName(matcher.group(1));
            if (!normalized.isEmpty()) {
                names.add(normalized);
            }
        }
        return names;
    }

    public List<String> resolveTagsForPost(Long postId, String content) {
        LinkedHashSet<String> fromContent = extractTagNames(content);
        if (!fromContent.isEmpty()) {
            return new ArrayList<>(fromContent);
        }
        return listTagNamesByPostId(postId);
    }

    @Transactional
    public void syncPostTagsIfMismatch(Long postId, String content) {
        if (postId == null) {
            return;
        }
        LinkedHashSet<String> fromContent = extractTagNames(content);
        List<String> fromDb = listTagNamesByPostId(postId);
        if (!fromContent.equals(new LinkedHashSet<>(fromDb))) {
            syncPostTags(postId, content);
        }
    }
}
