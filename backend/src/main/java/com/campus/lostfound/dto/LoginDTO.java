package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;

/** 登录请求（对齐 API 规格 2.2）。 */
public class LoginDTO {

    @NotBlank(message = "学号不能为空")
    private String studentNo;

    @NotBlank(message = "密码不能为空")
    private String password;

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
