package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 发起认领请求（API 规格 5.1）。 */
public class ClaimCreateDTO {

    @NotNull(message = "请选择物品")
    private Long itemId;

    @NotBlank(message = "请填写认领说明")
    @Size(max = 300, message = "认领说明不超过 300 字")
    private String detailDesc;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getDetailDesc() {
        return detailDesc;
    }

    public void setDetailDesc(String detailDesc) {
        this.detailDesc = detailDesc;
    }
}
