package com.campus.lostfound.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 审核请求（API 规格 7.2）：1 通过、2 驳回，驳回必填原因。 */
public class AuditDTO {

    @NotNull(message = "请选择审核结果")
    @Min(value = 1, message = "审核结果只能是 1 通过或 2 驳回")
    @Max(value = 2, message = "审核结果只能是 1 通过或 2 驳回")
    private Integer auditStatus;

    @Size(max = 100, message = "驳回原因不超过 100 字")
    private String rejectReason;

    public Integer getAuditStatus() {
        return auditStatus;
    }

    public void setAuditStatus(Integer auditStatus) {
        this.auditStatus = auditStatus;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}
