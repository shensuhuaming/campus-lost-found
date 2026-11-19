package com.campus.lostfound.dto;

import com.campus.lostfound.common.PageQuery;

/** 管理端用户查询（API 规格 7.7）。 */
public class AdminUserQuery extends PageQuery {

    private String keyword;
    private Integer role;
    private Integer status;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String trimmedKeyword() {
        return keyword == null ? "" : keyword.trim();
    }
}
