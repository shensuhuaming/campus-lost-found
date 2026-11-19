package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册请求（对齐 API 规格 2.1）。
 * <p>
 * 两次密码一致性的校验放在 Service：需要同时看两个字段，Bean Validation 单字段注解表达不了。
 */
public class RegisterDTO {

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "\\d{6,12}", message = "学号须为 6~12 位数字")
    private String studentNo;

    @NotBlank(message = "昵称不能为空")
    @Pattern(regexp = "\\S{2,12}", message = "昵称须为 2~12 个字符且不含空白")
    private String nickname;

    @NotBlank(message = "联系方式不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入正确的手机号")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度 6~20 位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "密码至少包含字母与数字")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
