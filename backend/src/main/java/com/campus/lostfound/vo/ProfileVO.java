package com.campus.lostfound.vo;

import com.campus.lostfound.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 个人中心概览（API 规格 2.4）：用户信息 + 四项统计。 */
public record ProfileVO(
        Long id,
        String studentNo,
        String nickname,
        String phone,
        Integer role,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt,
        Stats stat) {

    /** 我的发布数 / 我的认领数 / 待核验认领数 / 我的留言数。 */
    public record Stats(long published, long claiming, long claimingPending, long comments) {
    }

    public static ProfileVO of(User user, Stats stats) {
        return new ProfileVO(user.getId(), user.getStudentNo(), user.getNickname(),
                user.getPhone(), user.getRole(), user.getCreatedAt(), stats);
    }
}
