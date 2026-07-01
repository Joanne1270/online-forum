package com.forum.user.client;

import com.forum.common.dto.NotificationCreateRequest;
import com.forum.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/notifications/internal")
    Result<Void> create(@RequestBody NotificationCreateRequest request);
}
