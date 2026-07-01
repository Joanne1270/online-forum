package com.forum.user.controller;

import com.forum.common.dto.PageResult;
import com.forum.common.dto.Result;
import com.forum.user.dto.ProfileChangeRequestVO;
import com.forum.user.service.ProfileChangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/profile-requests")
public class ProfileChangeAdminController {

    private final ProfileChangeService profileChangeService;

    @GetMapping
    public Result<PageResult<ProfileChangeRequestVO>> list(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "20") int size) {
        return Result.ok(profileChangeService.listPending(page, size));
    }

    @PutMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        profileChangeService.approve(id);
        return Result.ok();
    }

    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        profileChangeService.reject(id);
        return Result.ok();
    }
}
