-- OA 系统 Phase 2a: 签到补签 - 数据库迁移脚本
-- 执行方式: mysql -u root -p day < 2026-07-21-add-retroactive-sign.sql

USE `day`;

CREATE TABLE IF NOT EXISTS `retroactive_sign` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `number` int(11) NOT NULL COMMENT '员工编号',
  `sign_date` varchar(50) NOT NULL COMMENT '补签日期(当天)',
  `type` varchar(10) NOT NULL COMMENT 'a=上午/p=下午',
  `reason` varchar(500) DEFAULT NULL COMMENT '补签原因',
  `status` varchar(20) NOT NULL COMMENT '待审批/已批准/已拒绝',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_number` (`number`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='签到补签申请表';
