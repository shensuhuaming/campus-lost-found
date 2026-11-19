package com.campus.lostfound.dto;

import lombok.Data;

/** 分类统计行：分类下信息数与已完成数。 */
@Data
public class CategoryStatRow {

    private Long categoryId;
    private String categoryName;
    private Long itemCount;
    private Long completedCount;
}
