-- =============================================================================
--  校园失物招领系统 —— 数据库物理设计脚本
--  文件：lost_found.sql
--  版本：V1.0        日期：2026-11
--  数据库：MySQL 8.0 / InnoDB / utf8mb4（utf8mb4_general_ci）
--  说明：
--    1) 本脚本可重复执行：先按外键依赖的逆序删除表，再依次创建 7 张业务表。
--    2) 脚本末尾写入 6 条物品分类的初始数据；管理员账号不在此处硬编码密文，
--       由应用启动时由 DataInitializer 幂等创建（见文末说明），避免明文/伪密文入库。
--    3) lost_item.close_reason 存放关闭/下架理由（管理员下架必填），与 reject_reason 语义分开
--    4) 所有表均使用 BIGINT 主键、DATETIME 时间戳，注释与字段一一对应
--       《详细设计说明书》5.x 节。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `lost_found`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;
USE `lost_found`;

DROP TABLE IF EXISTS `message`;
DROP TABLE IF EXISTS `comment`;
DROP TABLE IF EXISTS `claim`;
DROP TABLE IF EXISTS `item_image`;
DROP TABLE IF EXISTS `lost_item`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `user`;

SET FOREIGN_KEY_CHECKS = 1;

-- -----------------------------------------------------------------------------
-- 1. 用户表 user
--    学生与管理员共用一张表，以 role 区分；禁止物理删除，仅以 status 停用。
-- -----------------------------------------------------------------------------
CREATE TABLE `user` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_no` VARCHAR(20)     NOT NULL                COMMENT '学号/工号，登录账号，6~12 位',
    `password`   VARCHAR(100)    NOT NULL                COMMENT 'BCrypt 密文（$2a$10$...，60 字符）',
    `nickname`   VARCHAR(30)     NOT NULL                COMMENT '昵称，2~12 字',
    `phone`      VARCHAR(20)     NOT NULL                COMMENT '联系方式，5~20 位（手机号 / QQ / 微信）',
    `role`       TINYINT         NOT NULL DEFAULT 0      COMMENT '角色：0-学生 1-管理员',
    `status`     TINYINT         NOT NULL DEFAULT 0      COMMENT '状态：0-正常 1-禁用',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_student_no` (`student_no`),
    KEY `idx_user_role_status` (`role`, `status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '用户表（学生与管理员）';

-- -----------------------------------------------------------------------------
-- 2. 物品分类表 category
--    由管理员维护；存在关联失物信息时禁止删除（应用层校验 + RESTRICT 外键兜底）。
-- -----------------------------------------------------------------------------
CREATE TABLE `category` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(20)     NOT NULL                COMMENT '分类名称，唯一，如“证件”',
    `sort`       INT             NOT NULL DEFAULT 0      COMMENT '显示顺序，升序',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '物品分类表';

-- -----------------------------------------------------------------------------
-- 3. 失物信息表 lost_item
--    寻物/拾物共用一张表，以 type 区分；审核状态与物品状态彼此独立。
-- -----------------------------------------------------------------------------
CREATE TABLE `lost_item` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`          TINYINT         NOT NULL DEFAULT 1      COMMENT '类型：0-寻物 1-拾物',
    `title`         VARCHAR(30)     NOT NULL                COMMENT '标题，不超过 30 字',
    `description`   TEXT            NULL                    COMMENT '详细描述，不超过 500 字',
    `category_id`   BIGINT NOT NULL                COMMENT '分类 ID，外键 category.id',
    `user_id`       BIGINT NOT NULL                COMMENT '发布者 ID，外键 user.id',
    `happen_time`   DATETIME        NOT NULL                COMMENT '丢失/拾获时间',
    `happen_place`  VARCHAR(100)    NOT NULL                COMMENT '丢失/拾获地点',
    `contact`       VARCHAR(50)     NOT NULL                COMMENT '联系方式，仅登录用户可见',
    `audit_status`  TINYINT         NOT NULL DEFAULT 0      COMMENT '审核状态：0-待审核 1-通过 2-驳回',
    `reject_reason` VARCHAR(100)    NULL                    COMMENT '驳回原因，仅 audit_status=2 时有值，1~100 字',
    `close_reason`  VARCHAR(200)    NULL                    COMMENT '关闭/下架理由，管理员下架时必填，1~100 字',
    `item_status`   TINYINT         NOT NULL DEFAULT 0      COMMENT '物品状态：0-进行中 1-处理中 2-已完成 3-已关闭',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_item_audit_status` (`audit_status`, `created_at`),
    KEY `idx_item_type_status` (`type`, `item_status`),
    KEY `idx_item_category` (`category_id`),
    KEY `idx_item_user` (`user_id`),
    CONSTRAINT `fk_item_category` FOREIGN KEY (`category_id`) REFERENCES `category` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_item_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '失物信息表（寻物/拾物）';

-- -----------------------------------------------------------------------------
-- 4. 物品图片表 item_image
--    一条失物信息最多 3 张图片，以 sort 升序展示；url 存储相对路径 /uploads/...
-- -----------------------------------------------------------------------------
CREATE TABLE `item_image` (
    `id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_id` BIGINT NOT NULL                COMMENT '所属失物信息 ID',
    `url`     VARCHAR(255)    NOT NULL                COMMENT '图片相对 URL，如 /uploads/202611/uuid.png',
    `sort`    TINYINT         NOT NULL DEFAULT 0      COMMENT '展示顺序：0~2',
    PRIMARY KEY (`id`),
    KEY `idx_image_item` (`item_id`, `sort`),
    CONSTRAINT `fk_image_item` FOREIGN KEY (`item_id`) REFERENCES `lost_item` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '物品图片表';

-- -----------------------------------------------------------------------------
-- 5. 认领申请表 claim
--    同一用户对同一条失物信息只允许存在一条申请（唯一索引兜底，防并发重复提交）。
-- -----------------------------------------------------------------------------
CREATE TABLE `claim` (
    `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_id`      BIGINT NOT NULL                COMMENT '失物信息 ID',
    `applicant_id` BIGINT NOT NULL                COMMENT '申请人 ID',
    `detail_desc`  VARCHAR(300)    NOT NULL                COMMENT '认领说明，1~300 字',
    `status`       TINYINT         NOT NULL DEFAULT 0      COMMENT '核验状态：0-待核验 1-通过 2-拒绝',
    `reply`        VARCHAR(200)    NULL                    COMMENT '管理员回复/拒绝原因，≤200 字',
    `admin_id`     BIGINT NULL                    COMMENT '核验管理员 ID，未处理为 NULL',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `updated_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间（含核验时间）',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_claim_item_applicant` (`item_id`, `applicant_id`),
    KEY `idx_claim_status_created` (`status`, `created_at`),
    KEY `idx_claim_applicant` (`applicant_id`),
    KEY `idx_claim_admin` (`admin_id`),
    CONSTRAINT `fk_claim_item` FOREIGN KEY (`item_id`) REFERENCES `lost_item` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_claim_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT `fk_claim_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '认领申请表';

-- -----------------------------------------------------------------------------
-- 6. 留言表 comment
--    仅登录用户可发表；不设层级，按时间正序展示。
-- -----------------------------------------------------------------------------
CREATE TABLE `comment` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_id`    BIGINT NOT NULL                COMMENT '失物信息 ID',
    `user_id`    BIGINT NOT NULL                COMMENT '留言用户 ID',
    `content`    VARCHAR(200)    NOT NULL                COMMENT '留言内容，1~200 字',
    `created_at` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '留言时间',
    PRIMARY KEY (`id`),
    KEY `idx_comment_item` (`item_id`, `created_at`),
    KEY `idx_comment_user` (`user_id`),
    CONSTRAINT `fk_comment_item` FOREIGN KEY (`item_id`) REFERENCES `lost_item` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '留言表';

-- -----------------------------------------------------------------------------
-- 7. 站内消息表 message
--    from_user_id = 0 表示系统消息（因此该列不建外键，避免与 0 值冲突）。
--    type：1-审核结果 2-留言提醒 3-认领申请提醒 4-认领结果 5-系统通知
-- -----------------------------------------------------------------------------
CREATE TABLE `message` (
    `id`           BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `to_user_id`   BIGINT NOT NULL                COMMENT '接收人 ID',
    `from_user_id` BIGINT NOT NULL DEFAULT 0      COMMENT '发送人 ID，0 表示系统',
    `type`         TINYINT         NOT NULL                COMMENT '消息类型：1-审核结果 2-留言提醒 3-认领申请提醒 4-认领结果 5-系统通知',
    `content`      VARCHAR(500)    NOT NULL                COMMENT '消息正文',
    `item_id`      BIGINT NULL                    COMMENT '关联失物信息 ID，可为空',
    `is_read`      TINYINT         NOT NULL DEFAULT 0      COMMENT '是否已读：0-未读 1-已读',
    `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_msg_to_read` (`to_user_id`, `is_read`, `created_at`),
    CONSTRAINT `fk_msg_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `user` (`id`)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci
  COMMENT = '站内消息表';

-- -----------------------------------------------------------------------------
-- 8. 初始数据：物品分类（6 类，与《需求规格说明书》一致）
-- -----------------------------------------------------------------------------
INSERT INTO `category` (`name`, `sort`) VALUES
    ('证件', 1),
    ('电子产品', 2),
    ('钥匙', 3),
    ('书籍', 4),
    ('衣物', 5),
    ('其他', 6);

-- -----------------------------------------------------------------------------
-- 9. 管理员初始账号说明（不在脚本中硬编码密文）
--    账号：admin     初始密码：Admin@123
--    实现方式：应用启动时由 DataInitializer（CommandLineRunner）执行：
--      1) 查询 student_no = 'admin' 是否已存在，存在则跳过（幂等）；
--      2) 不存在则以 BCryptPasswordEncoder(cost = 10) 加密 "Admin@123" 后插入：
--         role = 1, status = 0, nickname = '系统管理员', phone = '00000000000'；
--      3) 首次登录后建议通过“修改密码”接口更换初始密码。
--    如需手工重置密码，可在后端工程中执行等价 Java 片段生成密文后执行 UPDATE：
--      String hash = new BCryptPasswordEncoder().encode("新密码");
--      -- UPDATE `user` SET `password` = '<hash>' WHERE `student_no` = 'admin';
-- -----------------------------------------------------------------------------

-- 校验脚本执行结果（可选）
-- SELECT TABLE_NAME, TABLE_COMMENT FROM information_schema.TABLES
--  WHERE TABLE_SCHEMA = 'lost_found' ORDER BY TABLE_NAME;