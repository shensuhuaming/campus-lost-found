package com.campus.lostfound.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.dto.LoginDTO;
import com.campus.lostfound.dto.RegisterDTO;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.RoleEnum;
import com.campus.lostfound.enums.UserStatusEnum;
import com.campus.lostfound.mapper.UserMapper;
import com.campus.lostfound.security.JwtUtil;
import com.campus.lostfound.security.SecurityUtil;
import com.campus.lostfound.service.AuthService;
import com.campus.lostfound.vo.LoginVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public Long register(RegisterDTO dto) {
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        if (countByStudentNo(dto.getStudentNo()) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS);        // 1002
        }

        User user = new User();
        user.setStudentNo(dto.getStudentNo());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));    // BCrypt cost=10
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setRole(RoleEnum.STUDENT.getCode());
        user.setStatus(UserStatusEnum.NORMAL.getCode());
        user.setCreatedAt(LocalDateTime.now());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {                            // 唯一索引并发兜底
            throw new BusinessException(ResultCode.USER_EXISTS);
        }
        log.info("user registered, id={}, studentNo={}", user.getId(), user.getStudentNo());
        return user.getId();
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, dto.getStudentNo()));
        // 账号不存在与密码错误返回同一提示，防止账号枚举
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);       // 1003
        }
        if (UserStatusEnum.DISABLED.getCode() == user.getStatus()) {
            throw new BusinessException(ResultCode.USER_DISABLED);      // 1004
        }
        String token = jwtUtil.generate(user.getId(), user.getRole());
        log.info("user login, id={}, role={}", user.getId(), user.getRole());
        return LoginVO.of(token, jwtUtil.getExpiresInSeconds(), user);
    }

    @Override
    public void logout() {
        // 无状态令牌：不写服务端会话，仅记录日志，前端负责清除 token
        log.info("user logout, uid={}", SecurityUtil.getUserId());
    }

    private long countByStudentNo(String studentNo) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStudentNo, studentNo));
    }
}
