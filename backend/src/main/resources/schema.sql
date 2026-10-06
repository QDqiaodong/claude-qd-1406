SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;

-- ---------------------------------------------------------------------
-- 体检中心 · 4 模块业务库
-- 库名 health_check；application.yml 里 ddl-auto: none，表结构全部由本文件在 MySQL 镜像构建期导入。
-- 注意：mysql 容器是 UTC，时间戳一律写 NOW()，日期用 DATE_SUB(CURDATE(), INTERVAL N DAY)。
-- 两处 schema（backend/src/main/resources/schema.sql 与 mysql/schema.sql）内容必须完全一致。
-- package 为 MySQL 保留字，所有标识符一律用反引号包裹。
-- ---------------------------------------------------------------------

CREATE DATABASE IF NOT EXISTS health_check
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE health_check;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `exam`;
DROP TABLE IF EXISTS `examinee`;
DROP TABLE IF EXISTS `package`;
DROP TABLE IF EXISTS `dept`;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------------- 1. 科室 dept（须在 exam 之前建立，供外键引用） ----------------
CREATE TABLE `dept` (
    `id`         BIGINT AUTO_INCREMENT PRIMARY KEY,
    `dept_no`    VARCHAR(32) UNIQUE COMMENT '科室编号，全局唯一',
    `dept_name`  VARCHAR(64) NOT NULL COMMENT '科室名，不可为空',
    `location`   VARCHAR(64) COMMENT '位置（门诊楼层）',
    `doctor`     VARCHAR(32) COMMENT '负责医师',
    `status`     VARCHAR(16) COMMENT '状态：开放 / 停诊 / 装修（停诊与装修不可排检查项）',
    `created_at` DATETIME COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 2. 体检套餐 package（须在 examinee 之前建立，供外键引用） ----------------
CREATE TABLE `package` (
    `id`           BIGINT AUTO_INCREMENT PRIMARY KEY,
    `package_name` VARCHAR(64) NOT NULL COMMENT '套餐名',
    `price`        DECIMAL(10,2) COMMENT '价格，必须大于 0',
    `item_count`   INT COMMENT '包含项目数，必须大于 0',
    `suitable`     VARCHAR(16) COMMENT '适用人群：入职体检 / 年度体检 / 老年体检 / 女性专项 / 儿童体检',
    `status`       VARCHAR(16) COMMENT '状态：在售 / 停售 / 下架（停售与下架不可被预约，下架为终态）',
    `created_at`   DATETIME COMMENT '创建时间',
    `updated_at`   DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 3. 体检人 examinee（依赖 package） ----------------
CREATE TABLE `examinee` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name`        VARCHAR(32) NOT NULL COMMENT '体检人姓名',
    `phone`       VARCHAR(20) COMMENT '手机号',
    `id_card`     VARCHAR(32) COMMENT '证件号',
    `gender`      VARCHAR(8) COMMENT '性别：男 / 女',
    `age`         INT COMMENT '年龄，0~120',
    `company`     VARCHAR(64) COMMENT '单位名称，可空（个人体检为空）',
    `package_id`  BIGINT COMMENT '体检套餐 id，必须在售才可预约',
    `status`      VARCHAR(16) COMMENT '状态：已预约 / 体检中 / 已完成 / 已取消（后两者为终态）',
    `created_at`  DATETIME COMMENT '创建时间',
    `updated_at`  DATETIME COMMENT '更新时间',
    CONSTRAINT `fk_examinee_package` FOREIGN KEY (`package_id`) REFERENCES `package` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------- 4. 检查项记录 exam（依赖 examinee + dept） ----------------
CREATE TABLE `exam` (
    `id`          BIGINT AUTO_INCREMENT PRIMARY KEY,
    `examinee_id` BIGINT COMMENT '体检人 id（须体检中才可做检查）',
    `dept_id`     BIGINT COMMENT '科室 id（须处于开放状态）',
    `item_name`   VARCHAR(16) COMMENT '检查项：身高体重 / 血压 / 血常规 / 尿常规 / 心电图 / 胸片 / B超 / 肝功能',
    `status`      VARCHAR(16) COMMENT '状态：待检 / 检查中 / 已完成 / 已弃检（后两者为终态）',
    `result`      VARCHAR(255) COMMENT '结果描述，可空；标记已完成、异常或弃检时必填',
    `abnormal`    TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否异常：1=异常 0=正常（异常必须填结果）',
    `check_time`  DATETIME COMMENT '检查完成时间，可空；标记已完成时写入',
    `created_at`  DATETIME COMMENT '创建时间',
    `updated_at`  DATETIME COMMENT '更新时间',
    CONSTRAINT `fk_exam_examinee` FOREIGN KEY (`examinee_id`) REFERENCES `examinee` (`id`),
    CONSTRAINT `fk_exam_dept` FOREIGN KEY (`dept_id`) REFERENCES `dept` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 种子数据（含异常态，用于真实触发业务拒绝分支）
--   异常态 1：KS006 口腔保健科「装修」→ 不可排检查项，且装修完成后只能恢复为开放
--   异常态 2：KS007 职业病科「停诊」→ 不可排检查项
--   异常态 3：P4 女性专项体检「停售」→ 不可被预约
--   异常态 4：P6 高管尊享体检「下架」→ 终态，不可被预约且不可再修改
--   异常态 5：E3 王强「已取消」→ 终态，不可做检查、不可再修改
--   异常态 6：E4 赵敏「已完成」→ 终态（其名下检查项全部已完成）
--   异常态 7：X9、X10 王强名下检查项「已弃检」→ 终态且必须填写理由
--   异常态 8：X12 血压偏高（abnormal=1）、X29 血红蛋白偏低（abnormal=1）→ 异常项已填结果
--   边界数据：E5 李学 9 岁、E6 孙丽 63 岁；P1 价格 280.00、项目数 6
-- ---------------------------------------------------------------------

INSERT IGNORE INTO `dept` (`id`, `dept_no`, `dept_name`, `location`, `doctor`, `status`, `created_at`, `updated_at`) VALUES
(1, 'KS001', '内科',       '门诊楼2层',   '张海涛', '开放', DATE_SUB(CURDATE(), INTERVAL 180 DAY), DATE_SUB(CURDATE(), INTERVAL 90 DAY)),
(2, 'KS002', '医学影像科', '医技楼1层',   '赵成峰', '开放', DATE_SUB(CURDATE(), INTERVAL 180 DAY), DATE_SUB(CURDATE(), INTERVAL 90 DAY)),
(3, 'KS003', '医学检验科', '医技楼2层',   '王思远', '开放', DATE_SUB(CURDATE(), INTERVAL 180 DAY), DATE_SUB(CURDATE(), INTERVAL 60 DAY)),
(4, 'KS004', '心电功能科', '门诊楼1层',   '周雅琴', '开放', DATE_SUB(CURDATE(), INTERVAL 150 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY)),
(5, 'KS005', '外科',       '门诊楼3层',   '李国栋', '开放', DATE_SUB(CURDATE(), INTERVAL 150 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY)),
(6, 'KS006', '口腔保健科', '门诊楼4层',   '孙倩',   '装修', DATE_SUB(CURDATE(), INTERVAL 100 DAY), DATE_SUB(CURDATE(), INTERVAL 5 DAY)),
(7, 'KS007', '职业病科',   '门诊楼5层',   '钱明辉', '停诊', DATE_SUB(CURDATE(), INTERVAL 100 DAY), DATE_SUB(CURDATE(), INTERVAL 20 DAY));

INSERT IGNORE INTO `package` (`id`, `package_name`, `price`, `item_count`, `suitable`, `status`, `created_at`, `updated_at`) VALUES
(1, '入职基础套餐', 280.00,  6,  '入职体检', '在售', DATE_SUB(CURDATE(), INTERVAL 120 DAY), DATE_SUB(CURDATE(), INTERVAL 30 DAY)),
(2, '年度标准套餐', 880.00,  10, '年度体检', '在售', DATE_SUB(CURDATE(), INTERVAL 120 DAY), DATE_SUB(CURDATE(), INTERVAL 20 DAY)),
(3, '银发关爱套餐', 1280.00, 12, '老年体检', '在售', DATE_SUB(CURDATE(), INTERVAL 90 DAY),  DATE_SUB(CURDATE(), INTERVAL 10 DAY)),
(4, '女性专项套餐', 1580.00, 8,  '女性专项', '停售', DATE_SUB(CURDATE(), INTERVAL 90 DAY),  DATE_SUB(CURDATE(), INTERVAL 15 DAY)),
(5, '儿童成长套餐', 660.00,  7,  '儿童体检', '在售', DATE_SUB(CURDATE(), INTERVAL 60 DAY),  DATE_SUB(CURDATE(), INTERVAL 8 DAY)),
(6, '高管尊享套餐', 2980.00, 14, '年度体检', '下架', DATE_SUB(CURDATE(), INTERVAL 200 DAY), DATE_SUB(CURDATE(), INTERVAL 100 DAY));

INSERT IGNORE INTO `examinee` (`id`, `name`, `phone`, `id_card`, `gender`, `age`, `company`, `package_id`, `status`, `created_at`, `updated_at`) VALUES
(1, '陈明', '13804010001', '310101199001011234', '男', 36, '星辰科技有限公司', 1, '已预约', DATE_SUB(CURDATE(), INTERVAL 2 DAY), DATE_SUB(CURDATE(), INTERVAL 2 DAY)),
(2, '刘芳', '13804010002', '310101198505054321', '女', 41, '江河教育集团',     2, '体检中', DATE_SUB(CURDATE(), INTERVAL 6 DAY), NOW()),
(3, '王强', '13804010003', '310101197403037890', '男', 52, NULL,               3, '已取消', DATE_SUB(CURDATE(), INTERVAL 9 DAY), DATE_SUB(CURDATE(), INTERVAL 4 DAY)),
(4, '赵敏', '13804010004', '310101199606066543', '女', 29, '云海传媒有限公司',  1, '已完成', DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
(5, '李学', '13804010005', '310101201707071234', '男',  9, '实验小学',          5, '体检中', DATE_SUB(CURDATE(), INTERVAL 3 DAY), NOW()),
(6, '孙丽', '13804010006', '310101196303038765', '女', 63, NULL,               3, '已预约', DATE_SUB(CURDATE(), INTERVAL 1 DAY), DATE_SUB(CURDATE(), INTERVAL 1 DAY));

INSERT IGNORE INTO `exam` (`id`, `examinee_id`, `dept_id`, `item_name`, `status`, `result`, `abnormal`, `check_time`, `created_at`, `updated_at`) VALUES
-- 刘芳（id=2，体检中）：8 项中 4 项已完成，进度 50%
(11, 2, 1, '身高体重', '已完成', '身高162cm，体重55kg，BMI 21.0，正常',            0, NOW(), DATE_SUB(CURDATE(), INTERVAL 6 DAY), NOW()),
(12, 2, 1, '血压',     '已完成', '血压 148/95mmHg 偏高，建议心内科复诊',          1, NOW(), DATE_SUB(CURDATE(), INTERVAL 6 DAY), NOW()),
(13, 2, 3, '血常规',   '已完成', '白细胞 5.8×10^9/L，血红蛋白 132g/L，正常',      0, NOW(), DATE_SUB(CURDATE(), INTERVAL 6 DAY), NOW()),
(14, 2, 3, '尿常规',   '已完成', '尿蛋白阴性，尿糖阴性，未见异常',                0, NOW(), DATE_SUB(CURDATE(), INTERVAL 6 DAY), NOW()),
(15, 2, 4, '心电图',   '检查中', NULL,                                            0, NULL,  DATE_SUB(CURDATE(), INTERVAL 6 DAY), NOW()),
(16, 2, 2, '胸片',     '待检',   NULL,                                            0, NULL,  DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_SUB(CURDATE(), INTERVAL 6 DAY)),
(17, 2, 3, '肝功能',   '待检',   NULL,                                            0, NULL,  DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_SUB(CURDATE(), INTERVAL 6 DAY)),
(18, 2, 2, 'B超',      '待检',   NULL,                                            0, NULL,  DATE_SUB(CURDATE(), INTERVAL 6 DAY), DATE_SUB(CURDATE(), INTERVAL 6 DAY)),
-- 王强（id=3，已取消）：全部检查项已弃检
(19, 3, 1, '血压',     '已弃检', '受检者主动取消体检，未测量血压',                0, NULL,  DATE_SUB(CURDATE(), INTERVAL 9 DAY), DATE_SUB(CURDATE(), INTERVAL 4 DAY)),
(20, 3, 3, '血常规',   '已弃检', '受检者取消体检，未能完成静脉采血',              0, NULL,  DATE_SUB(CURDATE(), INTERVAL 9 DAY), DATE_SUB(CURDATE(), INTERVAL 4 DAY)),
-- 赵敏（id=4，已完成）：6 项全部完成，服务端据此自动置为已完成
(21, 4, 1, '身高体重', '已完成', '身高165cm，体重58kg，BMI 21.3，正常',           0, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
(22, 4, 1, '血压',     '已完成', '血压 118/76mmHg，正常',                          0, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
(23, 4, 3, '血常规',   '已完成', '血小板 210×10^9/L，红细胞计数正常',             0, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
(24, 4, 3, '尿常规',   '已完成', '尿常规各项均为阴性，正常',                       0, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
(25, 4, 4, '心电图',   '已完成', '窦性心律，心率 72 次/分，正常',                  0, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
(26, 4, 3, '肝功能',   '已完成', 'ALT 22U/L，AST 20U/L，均在参考范围内',          0, DATE_SUB(CURDATE(), INTERVAL 7 DAY), DATE_SUB(CURDATE(), INTERVAL 14 DAY), DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
-- 李学（id=5，体检中）：4 项中 2 项完成，其中血常规异常
(27, 5, 1, '身高体重', '已完成', '身高132cm，体重28kg，发育正常',                 0, NOW(), DATE_SUB(CURDATE(), INTERVAL 3 DAY), NOW()),
(28, 5, 1, '血压',     '待检',   NULL,                                            0, NULL,  DATE_SUB(CURDATE(), INTERVAL 3 DAY), DATE_SUB(CURDATE(), INTERVAL 3 DAY)),
(29, 5, 3, '血常规',   '已完成', '血红蛋白 105g/L 偏低，轻度缺铁，建议补铁复检',  1, NOW(), DATE_SUB(CURDATE(), INTERVAL 3 DAY), NOW()),
(30, 5, 4, '心电图',   '检查中', NULL,                                            0, NULL,  DATE_SUB(CURDATE(), INTERVAL 3 DAY), NOW());
