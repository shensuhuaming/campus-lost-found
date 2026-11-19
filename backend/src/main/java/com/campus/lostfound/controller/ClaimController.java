package com.campus.lostfound.controller;

import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.ClaimCreateDTO;
import com.campus.lostfound.dto.ClaimQuery;
import com.campus.lostfound.dto.ClaimReplyDTO;
import com.campus.lostfound.service.ClaimService;
import com.campus.lostfound.vo.ClaimAdminVO;
import com.campus.lostfound.vo.ClaimResultVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认领接口（API 规格 5.1 ~ 5.4）。 */
@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    /** 发起认领（U4.2）。 */
    @PostMapping
    public Result<ClaimResultVO> create(@Valid @RequestBody ClaimCreateDTO dto) {
        return Result.ok(claimService.create(dto));
    }

    /** 核验列表（U4.5，管理员）。 */
    @GetMapping
    public Result<PageResult<ClaimAdminVO>> page(ClaimQuery query) {
        return Result.ok(claimService.page(query));
    }

    /** 核验通过（U4.3，管理员）。 */
    @PutMapping("/{id}/approve")
    public Result<ClaimResultVO> approve(@PathVariable Long id,
                                         @RequestBody(required = false) ClaimReplyDTO dto) {
        return Result.ok(claimService.approve(id, dto == null ? null : dto.getReply()));
    }

    /** 核验拒绝（U4.4，管理员）。 */
    @PutMapping("/{id}/reject")
    public Result<ClaimResultVO> reject(@PathVariable Long id,
                                        @Valid @RequestBody ClaimReplyDTO dto) {
        return Result.ok(claimService.reject(id, dto.getReply()));
    }
}
