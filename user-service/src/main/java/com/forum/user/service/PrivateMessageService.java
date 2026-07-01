package com.forum.user.service;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.PageResult;
import com.forum.common.exception.BusinessException;
import com.forum.user.dto.ConversationVO;
import com.forum.user.dto.PrivateMessageVO;
import com.forum.user.entity.PrivateMessage;
import com.forum.user.entity.User;
import com.forum.user.mapper.PrivateMessageMapper;
import com.forum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrivateMessageService {

    private static final int MAX_CONTENT_LENGTH = 2000;

    private final PrivateMessageMapper privateMessageMapper;
    private final UserMapper userMapper;

    public List<ConversationVO> listConversations(Long userId, String role) {
        denyAdminMessaging(role);
        List<PrivateMessage> latest = privateMessageMapper.listLatestConversations(userId);
        if (latest.isEmpty()) {
            return List.of();
        }
        List<ConversationVO> result = new ArrayList<>();
        for (PrivateMessage item : latest) {
            User peer = userMapper.findById(item.getPeerId());
            if (peer == null || ForumConstants.ROLE_ADMIN.equals(peer.getRole())
                    || (peer.getStatus() != null && peer.getStatus() == ForumConstants.USER_STATUS_DELETED)) {
                continue;
            }
            ConversationVO vo = new ConversationVO();
            vo.setPeerId(peer.getId());
            vo.setPeerNickname(peer.getNickname());
            vo.setPeerAvatar(peer.getAvatar());
            vo.setLastContent(item.getContent());
            vo.setLastTime(item.getCreatedAt());
            vo.setUnreadCount(privateMessageMapper.countUnreadWithPeer(userId, peer.getId()));
            result.add(vo);
        }
        return result;
    }

    public PageResult<PrivateMessageVO> listWithPeer(Long userId, String role, Long peerId, int page, int size) {
        denyAdminMessaging(role);
        validatePeer(userId, peerId);
        int offset = Math.max(page - 1, 0) * size;
        List<PrivateMessage> messages = privateMessageMapper.listWithPeer(userId, peerId, offset, size);
        privateMessageMapper.markReadWithPeer(userId, peerId);
        long total = privateMessageMapper.countWithPeer(userId, peerId);
        List<PrivateMessageVO> list = new ArrayList<>();
        for (PrivateMessage message : messages) {
            list.add(toVO(message, userId));
        }
        Collections.reverse(list);
        return new PageResult<>(list, total, page, size);
    }

    public PrivateMessageVO send(Long userId, String role, Long peerId, String content) {
        denyAdminMessaging(role);
        validatePeer(userId, peerId);
        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw new BusinessException("消息内容不能为空");
        }
        if (text.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("消息内容过长");
        }
        PrivateMessage message = new PrivateMessage();
        message.setSenderId(userId);
        message.setReceiverId(peerId);
        message.setContent(text);
        privateMessageMapper.insert(message);
        message.setReadFlag(0);
        return toVO(message, userId);
    }

    public void markRead(Long userId, String role, Long peerId) {
        denyAdminMessaging(role);
        validatePeer(userId, peerId);
        privateMessageMapper.markReadWithPeer(userId, peerId);
    }

    public long unreadCount(Long userId, String role) {
        if (ForumConstants.ROLE_ADMIN.equals(role)) {
            return 0;
        }
        return privateMessageMapper.countUnread(userId);
    }

    private void validatePeer(Long userId, Long peerId) {
        if (userId.equals(peerId)) {
            throw new BusinessException("不能给自己发私信");
        }
        User peer = userMapper.findById(peerId);
        if (peer == null) {
            throw new BusinessException("用户不存在");
        }
        if (ForumConstants.ROLE_ADMIN.equals(peer.getRole())) {
            throw new BusinessException("无法向管理员发送私信");
        }
        if (peer.getStatus() != null && peer.getStatus() == ForumConstants.USER_STATUS_DELETED) {
            throw new BusinessException("该用户已注销");
        }
    }

    private void denyAdminMessaging(String role) {
        if (ForumConstants.ROLE_ADMIN.equals(role)) {
            throw new BusinessException("管理员无法使用私信功能");
        }
    }

    private PrivateMessageVO toVO(PrivateMessage message, Long userId) {
        PrivateMessageVO vo = new PrivateMessageVO();
        vo.setId(message.getId());
        vo.setSenderId(message.getSenderId());
        vo.setReceiverId(message.getReceiverId());
        vo.setContent(message.getContent());
        vo.setRead(message.getReadFlag() != null && message.getReadFlag() == 1);
        vo.setCreatedAt(message.getCreatedAt());
        vo.setMine(userId.equals(message.getSenderId()));
        return vo;
    }
}
