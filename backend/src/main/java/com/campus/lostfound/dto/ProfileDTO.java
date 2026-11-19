package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 修改资料请求（API 规格 2.5）：昵称与联系方式均必填，学号/角色/状态不可改。 */
public class ProfileDTO {

    @NotBlank(message = "昵称不能为空")
    @Pattern(regexp = "\\S{2,12}", message = "昵称须为 2~12 个字符且不含空白")
    private String nickname;

    @NotBlank(message = "联系方式不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    private String phone;

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
