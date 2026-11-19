package com.campus.lostfound.init;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.lostfound.entity.User;
import com.campus.lostfound.enums.RoleEnum;
import com.campus.lostfound.enums.UserStatusEnum;
import com.campus.lostfound.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动时幂等创建管理员账号（详细设计 5.6）。
 * <p>
 * 不在 SQL 里硬编码 BCrypt 密文；口令默认 Admin@123，生产用环境变量 ADMIN_INIT_PASSWORD 覆盖。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.init-password:Admin@123}")
    private String initPassword;

    public DataInitializer(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Long admins = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getRole, RoleEnum.ADMIN.getCode()));
        if (admins != null && admins > 0) {
            return;
        }
        User admin = new User();
        admin.setStudentNo("admin");
        admin.setPassword(passwordEncoder.encode(initPassword));
        admin.setNickname("系统管理员");
        admin.setPhone("13800000000");
        admin.setRole(RoleEnum.ADMIN.getCode());
        admin.setStatus(UserStatusEnum.NORMAL.getCode());
        admin.setCreatedAt(LocalDateTime.now());
        userMapper.insert(admin);
        log.warn("已创建默认管理员 admin，初始口令来自配置，请登录后立即修改");
    }
}
