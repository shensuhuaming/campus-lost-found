package com.campus.lostfound.controller;

import com.campus.lostfound.common.PageQuery;
import com.campus.lostfound.common.PageResult;
import com.campus.lostfound.common.Result;
import com.campus.lostfound.dto.PasswordDTO;
import com.campus.lostfound.dto.ProfileDTO;
import com.campus.lostfound.service.UserService;
import com.campus.lostfound.vo.ClaimVO;
import com.campus.lostfound.vo.CommentVO;
import com.campus.lostfound.vo.ItemListVO;
import com.campus.lostfound.vo.ProfileVO;
import com.campus.lostfound.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 个人中心接口（API 规格 2.4 ~ 2.9）。 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 获取个人资料（含四项统计）。 */
    @GetMapping("/profile")
    public Result<ProfileVO> profile() {
        return Result.ok(userService.getProfile());
    }

    /** 修改昵称与联系方式。 */
    @PutMapping("/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody ProfileDTO dto) {
        return Result.ok(userService.updateProfile(dto));
    }

    /** 修改密码。 */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordDTO dto) {
        userService.changePassword(dto);
        return Result.ok();
    }

    /** 我的发布。 */
    @GetMapping("/items")
    public Result<PageResult<ItemListVO>> myItems(PageQuery query,
                                                  @RequestParam(required = false) Integer auditStatus,
                                                  @RequestParam(required = false) Integer itemStatus) {
        return Result.ok(userService.myItems(query, auditStatus, itemStatus));
    }

    /** 我的认领。 */
    @GetMapping("/claims")
    public Result<PageResult<ClaimVO>> myClaims(PageQuery query,
                                                @RequestParam(required = false) Integer status) {
        return Result.ok(userService.myClaims(query, status));
    }

    /** 我的留言。 */
    @GetMapping("/comments")
    public Result<PageResult<CommentVO>> myComments(PageQuery query) {
        return Result.ok(userService.myComments(query));
    }
}
