package com.campus.lostfound.service;

import com.campus.lostfound.dto.LoginDTO;
import com.campus.lostfound.dto.RegisterDTO;
import com.campus.lostfound.vo.LoginVO;

/** 认证服务：注册、登录、退出（详细设计 4.1.1 ~ 4.1.3）。 */
public interface AuthService {

    /** 学生注册，返回新用户 id；学号重复抛 1002。 */
    Long register(RegisterDTO dto);

    /** 登录并签发 JWT；账号不存在/密码错误 1003，禁用 1004。 */
    LoginVO login(LoginDTO dto);

    /** 退出：无状态令牌只记录日志，由前端清除 token。 */
    void logout();
}
