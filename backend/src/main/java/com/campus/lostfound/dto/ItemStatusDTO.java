package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotNull;

/** 寻物状态推进请求（API 规格 3.5）：1 处理中（已联系到拾到者），2 已完成（已找回）。 */
public class ItemStatusDTO {

    @NotNull(message = "请选择目标状态")
    private Integer targetStatus;

    public Integer getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(Integer targetStatus) {
        this.targetStatus = targetStatus;
    }
}
