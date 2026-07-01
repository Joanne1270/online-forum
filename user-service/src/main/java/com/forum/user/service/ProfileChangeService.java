package com.forum.user.service;

import com.forum.common.constant.ForumConstants;
import com.forum.common.dto.NotificationCreateRequest;
import com.forum.common.dto.PageResult;
import com.forum.common.exception.BusinessException;
import com.forum.user.client.NotificationClient;
import com.forum.user.dto.ProfileChangeRequestVO;
import com.forum.user.entity.ProfileChangeRequest;
import com.forum.user.entity.User;
import com.forum.user.mapper.ProfileChangeRequestMapper;
import com.forum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileChangeService {

    private final ProfileChangeRequestMapper profileChangeRequestMapper;
    private final UserMapper userMapper;
    private final NotificationClient notificationClient;

    public void submitChange(Long userId, String fieldType, String newValue) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        validateNewValue(userId, fieldType, newValue);
        String oldValue = ForumConstants.PROFILE_FIELD_NICKNAME.equals(fieldType) ? user.getNickname() : user.getAvatar();
        if (newValue.equals(oldValue)) {
            throw new BusinessException("内容未变更");
        }
        if (profileChangeRequestMapper.findPending(userId, fieldType) != null) {
            throw new BusinessException("已有待审核的修改申请，请等待管理员处理");
        }
        if (profileChangeRequestMapper.countTodayByUserAndField(userId, fieldType) >= ForumConstants.PROFILE_DAILY_LIMIT) {
            throw new BusinessException("今日修改次数已达上限（3次）");
        }
        ProfileChangeRequest request = new ProfileChangeRequest();
        request.setUserId(userId);
        request.setFieldType(fieldType);
        request.setOldValue(oldValue == null ? "" : oldValue);
        request.setNewValue(newValue);
        profileChangeRequestMapper.insert(request);
    }

    public void enrichProfileMeta(Long userId, com.forum.user.dto.UserProfileVO profile) {
        ProfileChangeRequest pendingNickname = profileChangeRequestMapper.findPending(userId, ForumConstants.PROFILE_FIELD_NICKNAME);
        ProfileChangeRequest pendingAvatar = profileChangeRequestMapper.findPending(userId, ForumConstants.PROFILE_FIELD_AVATAR);
        profile.setPendingNickname(pendingNickname != null ? pendingNickname.getNewValue() : null);
        profile.setPendingAvatar(pendingAvatar != null ? pendingAvatar.getNewValue() : null);
        profile.setNicknameChangesToday(profileChangeRequestMapper.countTodayByUserAndField(userId, ForumConstants.PROFILE_FIELD_NICKNAME));
        profile.setAvatarChangesToday(profileChangeRequestMapper.countTodayByUserAndField(userId, ForumConstants.PROFILE_FIELD_AVATAR));
    }

    public PageResult<ProfileChangeRequestVO> listPending(int page, int size) {
        int offset = (page - 1) * size;
        List<ProfileChangeRequestVO> list = profileChangeRequestMapper.listPending(offset, size).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, profileChangeRequestMapper.countPending(), page, size);
    }

    @Transactional
    public void approve(Long id) {
        ProfileChangeRequest request = requirePending(id);
        User user = userMapper.findById(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (ForumConstants.PROFILE_FIELD_NICKNAME.equals(request.getFieldType())) {
            User duplicate = userMapper.findByNickname(request.getNewValue());
            if (duplicate != null && !duplicate.getId().equals(request.getUserId())) {
                throw new BusinessException("昵称已被使用");
            }
            user.setNickname(request.getNewValue());
        } else if (ForumConstants.PROFILE_FIELD_AVATAR.equals(request.getFieldType())) {
            user.setAvatar(request.getNewValue());
        } else {
            throw new BusinessException("无效的申请类型");
        }
        userMapper.updateProfile(user);
        profileChangeRequestMapper.updateStatus(id, ForumConstants.PROFILE_REQUEST_APPROVED);
        notifyUser(request.getUserId(), ForumConstants.TAG_PROFILE_APPROVED,
                ForumConstants.PROFILE_FIELD_NICKNAME.equals(request.getFieldType()) ? "昵称" : "头像",
                true);
    }

    @Transactional
    public void reject(Long id) {
        ProfileChangeRequest request = requirePending(id);
        profileChangeRequestMapper.updateStatus(id, ForumConstants.PROFILE_REQUEST_REJECTED);
        notifyUser(request.getUserId(), ForumConstants.TAG_PROFILE_REJECTED,
                ForumConstants.PROFILE_FIELD_NICKNAME.equals(request.getFieldType()) ? "昵称" : "头像",
                false);
    }

    private ProfileChangeRequest requirePending(Long id) {
        ProfileChangeRequest request = profileChangeRequestMapper.findById(id);
        if (request == null || request.getStatus() != ForumConstants.PROFILE_REQUEST_PENDING) {
            throw new BusinessException("申请不存在或已处理");
        }
        return request;
    }

    private ProfileChangeRequestVO toVO(ProfileChangeRequest request) {
        ProfileChangeRequestVO vo = new ProfileChangeRequestVO();
        vo.setId(request.getId());
        vo.setUserId(request.getUserId());
        vo.setFieldType(request.getFieldType());
        vo.setOldValue(request.getOldValue());
        vo.setNewValue(request.getNewValue());
        vo.setStatus(request.getStatus());
        vo.setCreatedAt(request.getCreatedAt());
        User user = userMapper.findById(request.getUserId());
        if (user != null) {
            vo.setPhone(user.getPhone());
            vo.setNickname(user.getNickname());
        }
        return vo;
    }

    private void validateNewValue(Long userId, String fieldType, String newValue) {
        if (newValue == null || newValue.isBlank()) {
            throw new BusinessException("内容不能为空");
        }
        String value = newValue.trim();
        if (ForumConstants.PROFILE_FIELD_NICKNAME.equals(fieldType)) {
            if (value.length() > 50) {
                throw new BusinessException("昵称不能超过50字");
            }
            User duplicate = userMapper.findByNickname(value);
            if (duplicate != null && !duplicate.getId().equals(userId)) {
                throw new BusinessException("昵称已被使用");
            }
            return;
        }
        if (ForumConstants.PROFILE_FIELD_AVATAR.equals(fieldType)) {
            if (!value.startsWith("/api/files/uploads/")) {
                throw new BusinessException("请通过上传头像功能提交");
            }
            return;
        }
        throw new BusinessException("无效的申请类型");
    }

    private void notifyUser(Long userId, String type, String fieldLabel, boolean approved) {
        try {
            NotificationCreateRequest request = new NotificationCreateRequest();
            request.setUserId(userId);
            request.setType(type);
            request.setContent(approved
                    ? "您的" + fieldLabel + "修改申请已通过审核"
                    : "您的" + fieldLabel + "修改申请未通过审核");
            notificationClient.create(request);
        } catch (Exception ignored) {
        }
    }
}
