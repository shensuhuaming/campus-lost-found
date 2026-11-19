package com.campus.lostfound.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 启用/禁用用户请求（API 规格 7.8）。 */
public class UserStatusDTO {

    @NotNull(message = "请选择状态")
    @Min(value = 0, message = "状态只能是 0 正常或 1 禁用")
    @Max(value = 1, message = "状态只能是 0 正常或 1 禁用")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
