-- OA 系统 Phase 1: 请假管理 - 数据库迁移脚本
-- 执行方式: mysql -u root -p day < 2026-07-21-add-leave-type.sql

USE `day`;

ALTER TABLE `leave`
  ADD COLUMN `type` varchar(20) NOT NULL DEFAULT '事假' COMMENT '请假类型: 事假/病假/年假/调休'
  AFTER `name`;
