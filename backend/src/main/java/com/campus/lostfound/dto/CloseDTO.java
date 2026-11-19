package com.campus.lostfound.dto;

import jakarta.validation.constraints.Size;

/** 关闭信息请求（API 规格 3.3）：管理员下架必填理由，发布者可为空。 */
public class CloseDTO {

    @Size(max = 100, message = "关闭理由不超过 100 字")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
