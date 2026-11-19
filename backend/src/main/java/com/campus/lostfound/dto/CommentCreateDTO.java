package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 发表留言请求（API 规格 6.1）。 */
public class CommentCreateDTO {

    @NotNull(message = "请选择物品")
    private Long itemId;

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 200, message = "留言不超过 200 字")
    private String content;

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
