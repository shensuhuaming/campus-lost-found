package com.campus.lostfound.dto;

import com.campus.lostfound.common.PageQuery;

/** 列表/检索查询参数（API 规格 4.1）。 */
public class ItemQuery extends PageQuery {

    private Integer type;
    private Long categoryId;
    private String keyword;
    private Integer itemStatus;
    /** time（默认）或 relevance（标题命中优先） */
    private String sort;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(Integer itemStatus) {
        this.itemStatus = itemStatus;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String trimmedKeyword() {
        return keyword == null ? "" : keyword.trim();
    }
}
