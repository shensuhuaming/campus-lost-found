package com.campus.lostfound.vo;

/** 认领操作的返回（API 规格 5.1、5.3、5.4）：认领 id 与物品最新状态。 */
public record ClaimResultVO(Long claimId, Integer itemStatus) {
}
