package com.forum.admin.client;

import com.forum.admin.dto.AdminProfileRequestVO;
import com.forum.admin.dto.AdminUserVO;
import com.forum.admin.dto.OnlineStatsVO;
import com.forum.common.dto.BanRequest;
import com.forum.common.dto.PageResult;
import com.forum.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserAdminClient {

    @GetMapping("/api/users")
    Result<List<AdminUserVO>> listUsers(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/api/users/count")
    Result<Long> countUsers();

    @PutMapping("/api/users/{id}/ban")
    Result<Void> ban(@PathVariable("id") Long id, @RequestBody(required = false) BanRequest request);

    @PutMapping("/api/users/{id}/unban")
    Result<Void> unban(@PathVariable("id") Long id);

    @PutMapping("/api/users/{id}/role")
    Result<Void> setRole(@PathVariable("id") Long id, @RequestParam("role") String role);

    @DeleteMapping("/api/users/{id}")
    Result<Void> deactivate(@PathVariable("id") Long id);

    @GetMapping("/api/admin/profile-requests")
    Result<PageResult<AdminProfileRequestVO>> listProfileRequests(
            @RequestParam("page") int page, @RequestParam("size") int size);

    @PutMapping("/api/admin/profile-requests/{id}/approve")
    Result<Void> approveProfileRequest(@PathVariable("id") Long id);

    @PutMapping("/api/admin/profile-requests/{id}/reject")
    Result<Void> rejectProfileRequest(@PathVariable("id") Long id);

    @GetMapping("/api/admin/online-stats")
    Result<OnlineStatsVO> onlineStats(@RequestParam("range") String range);
}
