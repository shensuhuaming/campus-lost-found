-- =====================================================================
-- 校园失物招领系统 建表脚本（基线版，依据《需求规格说明书》V1.0 5.2 数据字典）
-- 约定：utf8mb4 / InnoDB / 主键 BIGINT AUTO_INCREMENT / 时间 DATETIME
-- 待确认项见 项目文档/需求理解与工程方案.md §2
-- =====================================================================

CREATE DATABASE IF NOT EXISTS campus_lost_found
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE campus_lost_found;

-- ------------------------- 用户表 -------------------------
CREATE TABLE IF NOT EXISTS `user`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `student_no` VARCHAR(20)  NOT NULL COMMENT '学号（登录账号）',
    `password`   VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 密文）',
    `nickname`   VARCHAR(30)  NOT NULL COMMENT '昵称',
    `phone`      VARCHAR(50)  DEFAULT NULL COMMENT '联系方式（手机号/QQ/微信）',
    `role`       TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0 学生，1 管理员',
    `status`     TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 正常，1 禁用',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_student_no` (`student_no`)
) ENGINE = InnoDB COMMENT = '用户表';

-- ------------------------- 分类表 -------------------------
CREATE TABLE IF NOT EXISTS `category`
(
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(20) NOT NULL COMMENT '分类名称',
    `sort`       INT         NOT NULL DEFAULT 0 COMMENT '排序值，越小越前',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_name` (`name`)
) ENGINE = InnoDB COMMENT = '物品分类表';

-- ------------------------- 物品信息表 -------------------------
CREATE TABLE IF NOT EXISTS `lost_item`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `type`          TINYINT      NOT NULL COMMENT '类型：0 寻物，1 拾物',
    `title`         VARCHAR(30)  NOT NULL COMMENT '标题（≤30 字）',
    `description`   VARCHAR(500) DEFAULT NULL COMMENT '物品描述（≤500 字）',
    `category_id`   BIGINT       NOT NULL COMMENT '分类ID',
    `user_id`       BIGINT       NOT NULL COMMENT '发布者ID',
    `happen_time`   DATETIME     DEFAULT NULL COMMENT '丢失/拾取时间',
    `happen_place`  VARCHAR(100) DEFAULT NULL COMMENT '丢失/拾取地点',
    `contact`       VARCHAR(50)  DEFAULT NULL COMMENT '联系方式',
    `audit_status`  TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态：0 待审核，1 通过，2 驳回',
    `reject_reason` VARCHAR(200) DEFAULT NULL COMMENT '驳回理由',
    `item_status`   TINYINT      NOT NULL DEFAULT 0 COMMENT '物品状态：0 进行中，1 处理中，2 已完成，3 已关闭',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_item_audit_status` (`audit_status`),
    KEY `idx_item_category` (`category_id`),
    KEY `idx_item_user` (`user_id`),
    KEY `idx_item_list` (`audit_status`, `item_status`, `type`, `created_at`)
) ENGINE = InnoDB COMMENT = '物品信息表（寻物启事 + 拾物招领）';

-- ------------------------- 物品图片表 -------------------------
CREATE TABLE IF NOT EXISTS `item_image`
(
    `id`      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_id` BIGINT       NOT NULL COMMENT '物品ID',
    `url`     VARCHAR(255) NOT NULL COMMENT '图片访问地址',
    `sort`    INT          NOT NULL DEFAULT 0 COMMENT '展示顺序，从 0 开始',
    PRIMARY KEY (`id`),
    KEY `idx_image_item` (`item_id`)
) ENGINE = InnoDB COMMENT = '物品图片表（每物品最多 3 张）';

-- ------------------------- 认领申请表 -------------------------
CREATE TABLE IF NOT EXISTS `claim`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_id`      BIGINT       NOT NULL COMMENT '物品ID',
    `applicant_id` BIGINT       NOT NULL COMMENT '申请人ID',
    `detail_desc`  VARCHAR(300) NOT NULL COMMENT '物品细节描述（用于核验身份）',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '申请状态：0 待核验，1 通过，2 拒绝',
    `reply`        VARCHAR(200) DEFAULT NULL COMMENT '管理员回复/拒绝理由',
    `admin_id`     BIGINT       DEFAULT NULL COMMENT '核验管理员ID',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_claim_item_applicant` (`item_id`, `applicant_id`),
    KEY `idx_claim_item` (`item_id`),
    KEY `idx_claim_applicant` (`applicant_id`)
) ENGINE = InnoDB COMMENT = '认领申请表';

-- ------------------------- 留言表 -------------------------
CREATE TABLE IF NOT EXISTS `comment`
(
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `item_id`    BIGINT       NOT NULL COMMENT '物品ID',
    `user_id`    BIGINT       NOT NULL COMMENT '留言人ID',
    `content`    VARCHAR(500) NOT NULL COMMENT '留言内容',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '留言时间',
    PRIMARY KEY (`id`),
    KEY `idx_comment_item` (`item_id`)
) ENGINE = InnoDB COMMENT = '物品留言表';

-- ------------------------- 站内消息表 -------------------------
CREATE TABLE IF NOT EXISTS `message`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `to_user_id`   BIGINT       NOT NULL COMMENT '接收人ID',
    `from_user_id` BIGINT       NOT NULL DEFAULT 0 COMMENT '发送人ID，0 表示系统',
    `type`         TINYINT      NOT NULL COMMENT '消息类型（取值域待接口契约确认）',
    `content`      VARCHAR(255) NOT NULL COMMENT '消息内容',
    `item_id`      BIGINT       DEFAULT NULL COMMENT '关联物品ID，可为空',
    `is_read`      TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读：0 未读，1 已读',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_message_to_read` (`to_user_id`, `is_read`, `created_at`)
) ENGINE = InnoDB COMMENT = '站内消息表';
