package com.campus.lostfound.dto;

import com.campus.lostfound.common.PageQuery;

/** 认领核验列表查询（API 规格 5.2）。 */
public class ClaimQuery extends PageQuery {

    /** 0 待核验（默认）、1 通过、2 拒绝 */
    private Integer status;
    private String keyword;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int safeStatus() {
        return status == null ? 0 : status;
    }

    public String trimmedKeyword() {
        return keyword == null ? "" : keyword.trim();
    }
}
