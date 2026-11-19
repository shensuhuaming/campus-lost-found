package com.campus.lostfound.util;

import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.enums.ClaimStatusEnum;

import java.util.Map;
import java.util.Set;

/**
 * 物品与认领状态机（详细设计 4.4.1）。
 * <p>
 * 所有状态变更必须先经过这里，非法迁移统一抛业务异常（物品 2003、认领 3002）。
 */
public final class ItemStatusMachine {

    /** 合法迁移：进行中 → 处理中/已关闭；处理中 → 进行中/已完成/已关闭；已完成与已关闭为终态。 */
    private static final Map<Integer, Set<Integer>> ITEM_ALLOWED = Map.of(
            0, Set.of(1, 3),
            1, Set.of(0, 2, 3),
            2, Set.of(),
            3, Set.of());

    private ItemStatusMachine() {
    }

    public static void checkItem(Integer from, Integer to) {
        if (!ITEM_ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new BusinessException(ResultCode.ITEM_STATUS_ILLEGAL);
        }
    }

    public static void checkClaim(Integer from) {
        if (from == null || from != ClaimStatusEnum.PENDING.getCode()) {
            throw new BusinessException(ResultCode.CLAIM_STATUS_ILLEGAL);
        }
    }

    public static void checkClose(Integer from) {
        checkItem(from, 3);
    }
}
