package com.campus.lostfound.vo;

import java.util.List;

/** 数据统计（API 规格 7.9）。 */
public record StatsVO(
        long totalItems,
        long pendingAudit,
        long completedClaims,
        double returnRate,
        List<CategoryStat> categoryStats,
        List<TrendPoint> trend) {

    /** 分类维度：信息数、已完成数、归还率。 */
    public record CategoryStat(Long categoryId, String categoryName, long itemCount,
                               long completedCount, double returnRate) {
    }

    /** 每日发布量。 */
    public record TrendPoint(String date, long count) {
    }
}
