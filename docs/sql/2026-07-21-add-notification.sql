-- OA 系统 Phase 2b: 通知推送 - 数据库迁移脚本
-- 执行方式: mysql -u root -p day < 2026-07-21-add-notification.sql

USE `day`;

CREATE TABLE IF NOT EXISTS `notification` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` varchar(20) NOT NULL COMMENT 'leave_approved/leave_rejected/retroactive_approved/retroactive_rejected/sign_remind',
  `title` varchar(200) NOT NULL,
  `content` text,
  `target_number` int(11) NOT NULL COMMENT '接收通知的员工编号',
  `biz_id` varchar(50) DEFAULT NULL COMMENT '关联业务ID(请假ID等)',
  `is_read` tinyint(1) NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_target` (`target_number`,`is_read`),
  KEY `idx_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='通知消息表';
