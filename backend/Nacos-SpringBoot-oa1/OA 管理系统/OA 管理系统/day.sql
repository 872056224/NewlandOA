/*
Navicat MySQL Data Transfer

Source Server         : localhost
Source Server Version : 50710
Source Host           : localhost:3306
Source Database       : day

Target Server Type    : MYSQL
Target Server Version : 50710
File Encoding         : 65001

Date: 2026-06-09 17:12:47
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for `admin`
-- ----------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(10) NOT NULL,
  `pwd` varchar(50) NOT NULL DEFAULT '202cb962ac59075b964b07152d234b70',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=10014 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of admin
-- ----------------------------
INSERT INTO `admin` VALUES ('10001', 'chenle', '123123');
INSERT INTO `admin` VALUES ('10002', 'zhanghong', '4297f44b13955235245b2497399d7a93');
INSERT INTO `admin` VALUES ('10003', 'yanjie', '4297f44b13955235245b2497399d7a93');
INSERT INTO `admin` VALUES ('10004', 'liuping', '4297f44b13955235245b2497399d7a93');
INSERT INTO `admin` VALUES ('10005', 'chenle@987', '4297f44b13955235245b2497399d7a93');
INSERT INTO `admin` VALUES ('10006', 'yidan', '4297f44b13955235245b2497399d7a93');
INSERT INTO `admin` VALUES ('10007', 'zhangsan', '123123');
INSERT INTO `admin` VALUES ('10008', 'lisi', '4297f44b13955235245b2497399d7a93');
INSERT INTO `admin` VALUES ('10009', 'xiaoming', '4297f44b13955235245b2497399d7a93');
INSERT INTO `admin` VALUES ('10010', 'xingxing', '4297f44b13955235245b2497399d7a93');
INSERT INTO `admin` VALUES ('10011', 'test', '202cb962ac59075b964b07152d234b70');
INSERT INTO `admin` VALUES ('10012', 'xingcheng', '4297f44b13955235245b2497399d7a93');
INSERT INTO `admin` VALUES ('10013', 'yueliang', '4297f44b13955235245b2497399d7a93');

-- ----------------------------
-- Table structure for `department`
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department` (
  `dept_id` int(11) NOT NULL AUTO_INCREMENT,
  `dept_name` varchar(20) NOT NULL,
  PRIMARY KEY (`dept_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of department
-- ----------------------------
INSERT INTO `department` VALUES ('1', '人事部');
INSERT INTO `department` VALUES ('2', '后勤部');
INSERT INTO `department` VALUES ('3', 'Java研发部');
INSERT INTO `department` VALUES ('4', '商品部');
INSERT INTO `department` VALUES ('5', '销售部');
INSERT INTO `department` VALUES ('6', '行政部');
INSERT INTO `department` VALUES ('17', '大数据研发部');
INSERT INTO `department` VALUES ('18', '前端研发部');
INSERT INTO `department` VALUES ('19', '测试部');

-- ----------------------------
-- Table structure for `duty`
-- ----------------------------
DROP TABLE IF EXISTS `duty`;
CREATE TABLE `duty` (
  `duty_id` int(11) NOT NULL AUTO_INCREMENT,
  `duty_name` varchar(20) NOT NULL,
  PRIMARY KEY (`duty_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of duty
-- ----------------------------
INSERT INTO `duty` VALUES ('1', '部长');
INSERT INTO `duty` VALUES ('3', '组长');
INSERT INTO `duty` VALUES ('4', '副组长');
INSERT INTO `duty` VALUES ('5', '普通员工');
INSERT INTO `duty` VALUES ('9', 'Java软件工程师');
INSERT INTO `duty` VALUES ('10', 'Java软件架构师');
INSERT INTO `duty` VALUES ('16', 'Web前端工程师');

-- ----------------------------
-- Table structure for `emp`
-- ----------------------------
DROP TABLE IF EXISTS `emp`;
CREATE TABLE `emp` (
  `number` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(10) NOT NULL,
  `pwd` varchar(50) NOT NULL DEFAULT '202cb962ac59075b964b07152d234b70',
  `birthday` varchar(20) NOT NULL,
  `address` varchar(30) NOT NULL,
  `dept_id` int(11) NOT NULL,
  `duty_id` int(11) NOT NULL,
  PRIMARY KEY (`number`) USING BTREE,
  KEY `dept_id` (`dept_id`) USING BTREE,
  KEY `duty_id` (`duty_id`) USING BTREE,
  CONSTRAINT `emp_ibfk_1` FOREIGN KEY (`dept_id`) REFERENCES `department` (`dept_id`),
  CONSTRAINT `emp_ibfk_2` FOREIGN KEY (`duty_id`) REFERENCES `duty` (`duty_id`)
) ENGINE=InnoDB AUTO_INCREMENT=155 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of emp
-- ----------------------------
INSERT INTO `emp` VALUES ('121', '陈乐', '202cb962ac59075b964b07152d234b70', '1998-09-01', '福州市连江县', '3', '10');
INSERT INTO `emp` VALUES ('123', '吴星星', '4297f44b13955235245b2497399d7a93', '1998-09-03', '福建省福州市连江县', '3', '5');
INSERT INTO `emp` VALUES ('127', '陈乐', '4297f44b13955235245b2497399d7a93', '1999-06-09', '福建省福州市', '4', '4');
INSERT INTO `emp` VALUES ('128', '许褚', '4297f44b13955235245b2497399d7a93', '1998-06-22', '福建省福州市', '3', '5');
INSERT INTO `emp` VALUES ('129', '马超', '202cb962ac59075b964b07152d234b70', '2001-03-01', '福建省福州市', '5', '5');
INSERT INTO `emp` VALUES ('134', '孙坚', '202cb962ac59075b964b07152d234b70', '2010-05-04', '福建省宁德市', '3', '4');
INSERT INTO `emp` VALUES ('136', '黄盖', '202cb962ac59075b964b07152d234b70', '2001-03-05', '福建省漳州市', '2', '3');
INSERT INTO `emp` VALUES ('147', '吴星星', '202cb962ac59075b964b07152d234b70', '1998-09-08', '福建省福州市', '3', '5');
INSERT INTO `emp` VALUES ('148', '严洁', '202cb962ac59075b964b07152d234b70', '1993-05-05', '福建省福州市', '6', '5');
INSERT INTO `emp` VALUES ('149', '郑少涵', '202cb962ac59075b964b07152d234b70', '2006-11-09', '福建省南平市', '1', '5');
INSERT INTO `emp` VALUES ('153', '赵六', '4297f44b13955235245b2497399d7a93', '2025-12-01', '福建省漳州市', '3', '5');
INSERT INTO `emp` VALUES ('154', '张虹', '4297f44b13955235245b2497399d7a93', '2013-05-14', '福建省福州市', '1', '3');

-- ----------------------------
-- Table structure for `leave`
-- ----------------------------
DROP TABLE IF EXISTS `leave`;
CREATE TABLE `leave` (
  `id` varchar(36) NOT NULL,
  `number` int(11) NOT NULL,
  `name` varchar(50) NOT NULL,
  `dept_name` varchar(50) NOT NULL,
  `start_date` datetime NOT NULL,
  `end_date` datetime NOT NULL,
  `reason` text NOT NULL,
  `status` varchar(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of leave
-- ----------------------------
INSERT INTO `leave` VALUES ('3028436a-78d8-4af8-a450-879d16dae3cd', '121', '陈乐', '技术部', '2025-07-29 16:00:00', '2025-07-30 16:00:00', '请假一天，申请去苏州游玩。望领导批准', '已拒绝');
INSERT INTO `leave` VALUES ('4645f912-e06d-4c3a-b42d-eda2d60544d1', '122', '关云长', '技术部', '2025-07-30 00:00:00', '2025-07-30 16:00:00', '去苏州游玩。望领导批准', '已批准');
INSERT INTO `leave` VALUES ('b2aa0164-0916-41f3-87c7-92e470e3c6bb', '123', '吴星星', 'Java研发部', '2026-01-27 00:30:00', '2026-01-27 09:30:00', '我也要去日本啦', '已批准');
INSERT INTO `leave` VALUES ('c589bcfd-6840-4a7b-9584-1b2f2311ea69', '147', '星星', '行政部', '2025-07-27 16:00:00', '2025-07-28 16:00:00', '福州三分银饰店铺开业', '已批准');
INSERT INTO `leave` VALUES ('dd80b232-eef2-4bbc-9562-dc93d6304289', '127', '陈乐', '商品部', '2026-01-27 00:30:00', '2026-01-27 09:30:00', '需要去日本', '已批准');

-- ----------------------------
-- Table structure for `sign`
-- ----------------------------
DROP TABLE IF EXISTS `sign`;
CREATE TABLE `sign` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `signDate` varchar(50) NOT NULL,
  `number` int(11) NOT NULL,
  `state` varchar(10) NOT NULL,
  `type` varchar(10) NOT NULL,
  `sign_address` varchar(200) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `number` (`number`) USING BTREE,
  CONSTRAINT `sign_ibfk_1` FOREIGN KEY (`number`) REFERENCES `emp` (`number`)
) ENGINE=InnoDB AUTO_INCREMENT=1837 DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

-- ----------------------------
-- Records of sign
-- ----------------------------
INSERT INTO `sign` VALUES ('355', '2023-06-05 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('356', '2023-06-05 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('357', '2023-06-05 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('358', '2023-06-05 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('361', '2023-06-05 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('362', '2023-06-05 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('365', '2023-06-05 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('366', '2023-06-05 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('369', '2023-06-05 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('370', '2023-06-05 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('377', '2023-06-05 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('378', '2023-06-05 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('383', '2023-06-05 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('384', '2023-06-05 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('393', '2023-06-06 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('394', '2023-06-06 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('395', '2023-06-06 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('396', '2023-06-06 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('399', '2023-06-06 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('400', '2023-06-06 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('403', '2023-06-06 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('404', '2023-06-06 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('407', '2023-06-06 16:23:28:612', '123', '已签到', 'a', '福建省福州市马尾区');
INSERT INTO `sign` VALUES ('408', '2023-06-06 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('415', '2023-06-06 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('416', '2023-06-06 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('421', '2023-06-06 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('422', '2023-06-06 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('431', '2023-06-07 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('432', '2023-06-07 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('433', '2023-06-07 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('434', '2023-06-07 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('437', '2023-06-07 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('438', '2023-06-07 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('441', '2023-06-07 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('442', '2023-06-07 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('445', '2023-06-07 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('446', '2023-06-07 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('453', '2023-06-07 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('454', '2023-06-07 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('459', '2023-06-07 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('460', '2023-06-07 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('469', '2023-06-09 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('470', '2023-06-09 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('471', '2023-06-09 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('472', '2023-06-09 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('475', '2023-06-09 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('476', '2023-06-09 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('479', '2023-06-09 10:22:59:09', '121', '已签到', 'a', '福建省福州市马尾区');
INSERT INTO `sign` VALUES ('480', '2023-06-09 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('483', '2023-06-09 09:43:41:334', '123', '已签到', 'a', '福建省福州市马尾区');
INSERT INTO `sign` VALUES ('484', '2023-06-09 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('491', '2023-06-09 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('492', '2023-06-09 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('497', '2023-06-09 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('498', '2023-06-09 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('507', '2023-06-10 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('508', '2023-06-10 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('509', '2023-06-10 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('510', '2023-06-10 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('513', '2023-06-10 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('514', '2023-06-10 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('517', '2023-06-10 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('518', '2023-06-10 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('521', '2023-06-10 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('522', '2023-06-10 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('529', '2023-06-10 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('530', '2023-06-10 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('535', '2023-06-10 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('536', '2023-06-10 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('539', '2023-06-13 21:34:49:69', '123', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('540', '2023-06-13 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('547', '2023-06-13 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('548', '2023-06-13 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('549', '2023-06-13 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('550', '2023-06-13 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('553', '2023-06-13 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('554', '2023-06-13 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('557', '2023-06-13 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('558', '2023-06-13 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('567', '2023-06-13 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('568', '2023-06-13 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('573', '2023-06-13 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('574', '2023-06-13 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('577', '2023-06-14 19:09:36:804', '123', '已签到', 'a', '江苏省南通市崇川区');
INSERT INTO `sign` VALUES ('578', '2023-06-14 19:16:35:289', '123', '已签到', 'p', '江苏省南通市崇川区');
INSERT INTO `sign` VALUES ('585', '2023-06-14 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('586', '2023-06-14 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('587', '2023-06-14 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('588', '2023-06-14 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('591', '2023-06-14 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('592', '2023-06-14 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('595', '2023-06-14 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('596', '2023-06-14 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('605', '2023-06-14 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('606', '2023-06-14 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('611', '2023-06-14 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('612', '2023-06-14 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('615', '2023-06-15 08:49:52:557', '123', '已签到', 'a', '江苏省南通市崇川区');
INSERT INTO `sign` VALUES ('616', '2023-06-15 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('623', '2023-06-15 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('624', '2023-06-15 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('625', '2023-06-15 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('626', '2023-06-15 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('629', '2023-06-15 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('630', '2023-06-15 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('633', '2023-06-15 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('634', '2023-06-15 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('643', '2023-06-15 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('644', '2023-06-15 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('649', '2023-06-15 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('650', '2023-06-15 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('733', '2023-06-16 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('734', '2023-06-16 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('741', '2023-06-16 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('742', '2023-06-16 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('743', '2023-06-16 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('744', '2023-06-16 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('747', '2023-06-16 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('748', '2023-06-16 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('751', '2023-06-16 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('752', '2023-06-16 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('759', '2023-06-16 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('760', '2023-06-16 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('765', '2023-06-16 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('766', '2023-06-16 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('769', '2023-06-19 14:53:40:192', '123', '已签到', 'a', '福建省福州市鼓楼区');
INSERT INTO `sign` VALUES ('770', '2023-06-19 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('777', '2023-06-19 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('778', '2023-06-19 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('779', '2023-06-19 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('780', '2023-06-19 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('783', '2023-06-19 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('784', '2023-06-19 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('787', '2023-06-19 15:08:22:125', '121', '已签到', 'a', '福建省福州市鼓楼区');
INSERT INTO `sign` VALUES ('788', '2023-06-19 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('795', '2023-06-19 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('796', '2023-06-19 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('801', '2023-06-19 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('802', '2023-06-19 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('805', '2023-06-28 19:57:06:885', '123', '已签到', 'a', '福建省福州市台江区');
INSERT INTO `sign` VALUES ('806', '2023-06-28 20:19:21:519', '123', '已签到', 'p', '福建省福州市台江区');
INSERT INTO `sign` VALUES ('813', '2023-06-28 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('814', '2023-06-28 14:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('815', '2023-06-28 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('816', '2023-06-28 14:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('819', '2023-06-28 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('820', '2023-06-28 14:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('823', '2023-06-28 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('824', '2023-06-28 14:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('831', '2023-06-28 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('832', '2023-06-28 14:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('837', '2023-06-28 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('838', '2023-06-28 14:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('841', '2023-07-03 09:52:24:339', '123', '已签到', 'a', '福建省福州市马尾区');
INSERT INTO `sign` VALUES ('842', '2023-07-03 14:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('849', '2023-07-03 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('850', '2023-07-03 14:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('851', '2023-07-03 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('852', '2023-07-03 14:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('855', '2023-07-03 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('856', '2023-07-03 14:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('859', '2023-07-03 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('860', '2023-07-03 14:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('867', '2023-07-03 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('868', '2023-07-03 14:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('873', '2023-07-03 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('874', '2023-07-03 14:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('877', '2023-07-04 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('878', '2023-07-04 14:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('885', '2023-07-04 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('886', '2023-07-04 14:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('887', '2023-07-04 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('888', '2023-07-04 14:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('891', '2023-07-04 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('892', '2023-07-04 14:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('893', '2023-07-04 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('894', '2023-07-04 14:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('903', '2023-07-04 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('904', '2023-07-04 14:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('909', '2023-07-04 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('910', '2023-07-04 14:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('913', '2023-09-27 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('914', '2023-09-27 14:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('921', '2023-09-27 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('922', '2023-09-27 14:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('923', '2023-09-27 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('924', '2023-09-27 14:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('927', '2023-09-27 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('928', '2023-09-27 14:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('929', '2023-09-27 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('930', '2023-09-27 14:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('939', '2023-09-27 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('940', '2023-09-27 14:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('945', '2023-09-27 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('946', '2023-09-27 14:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('949', '2023-10-11 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('950', '2023-10-11 14:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('957', '2023-10-11 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('958', '2023-10-11 14:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('959', '2023-10-11 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('960', '2023-10-11 14:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('963', '2023-10-11 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('964', '2023-10-11 14:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('965', '2023-10-11 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('966', '2023-10-11 14:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('975', '2023-10-11 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('976', '2023-10-11 14:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('981', '2023-10-11 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('982', '2023-10-11 14:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('985', '2023-10-12 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('986', '2023-10-12 14:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('993', '2023-10-12 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('994', '2023-10-12 14:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('997', '2023-10-12 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('998', '2023-10-12 14:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('999', '2023-10-12 10:33:36:465', '121', '已签到', 'a', '福建省福州市马尾区');
INSERT INTO `sign` VALUES ('1000', '2023-10-12 14:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1003', '2023-10-12 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1004', '2023-10-12 14:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1011', '2023-10-12 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1012', '2023-10-12 14:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1017', '2023-10-12 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1018', '2023-10-12 14:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1021', '2024-05-14 11:06:46:79', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1022', '2024-05-14 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1029', '2024-05-14 11:09:11:899', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1030', '2024-05-14 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1033', '2024-05-14 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1034', '2024-05-14 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1035', '2024-05-14 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1036', '2024-05-14 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1039', '2024-05-14 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1040', '2024-05-14 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1045', '2024-05-14 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1046', '2024-05-14 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1049', '2024-05-14 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1050', '2024-05-14 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1057', '2024-05-26 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1058', '2024-05-26 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1061', '2024-05-26 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1062', '2024-05-26 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1069', '2024-05-26 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1070', '2024-05-26 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1073', '2024-05-26 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1074', '2024-05-26 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1075', '2024-05-26 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1076', '2024-05-26 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1081', '2024-05-26 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1082', '2024-05-26 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1085', '2024-05-26 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1086', '2024-05-26 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1089', '2024-11-19 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1090', '2024-11-19 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1093', '2024-11-19 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1094', '2024-11-19 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1097', '2024-11-19 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1098', '2024-11-19 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1101', '2024-11-19 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1102', '2024-11-19 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1103', '2024-11-19 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1104', '2024-11-19 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1109', '2024-11-19 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1110', '2024-11-19 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1111', '2024-11-19 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1112', '2024-11-19 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1115', '2024-12-23 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1116', '2024-12-23 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1119', '2024-12-23 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1120', '2024-12-23 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1123', '2024-12-23 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1124', '2024-12-23 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1127', '2024-12-23 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1128', '2024-12-23 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1129', '2024-12-23 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1130', '2024-12-23 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1135', '2024-12-23 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1136', '2024-12-23 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1137', '2024-12-23 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1138', '2024-12-23 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1143', '2025-06-11 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1144', '2025-06-11 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1147', '2025-06-11 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1148', '2025-06-11 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1151', '2025-06-11 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1152', '2025-06-11 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1153', '2025-06-11 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1154', '2025-06-11 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1155', '2025-06-11 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1156', '2025-06-11 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1161', '2025-06-11 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1162', '2025-06-11 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1163', '2025-06-11 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1164', '2025-06-11 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1169', '2025-06-12 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1170', '2025-06-12 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1173', '2025-06-12 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1174', '2025-06-12 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1177', '2025-06-12 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1178', '2025-06-12 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1179', '2025-06-12 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1180', '2025-06-12 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1181', '2025-06-12 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1182', '2025-06-12 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1187', '2025-06-12 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1188', '2025-06-12 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1189', '2025-06-12 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1190', '2025-06-12 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1195', '2025-06-15 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1196', '2025-06-15 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1199', '2025-06-15 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1200', '2025-06-15 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1201', '2025-06-15 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1202', '2025-06-15 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1203', '2025-06-15 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1204', '2025-06-15 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1205', '2025-06-15 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1206', '2025-06-15 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1211', '2025-06-15 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1212', '2025-06-15 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1213', '2025-06-15 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1214', '2025-06-15 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1221', '2025-06-17 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1222', '2025-06-17 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1225', '2025-06-17 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1226', '2025-06-17 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1227', '2025-06-17 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1228', '2025-06-17 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1229', '2025-06-17 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1230', '2025-06-17 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1231', '2025-06-17 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1232', '2025-06-17 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1237', '2025-06-17 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1238', '2025-06-17 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1239', '2025-06-17 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1240', '2025-06-17 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1247', '2025-06-20 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1248', '2025-06-20 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1251', '2025-06-20 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1252', '2025-06-20 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1255', '2025-06-20 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1256', '2025-06-20 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1257', '2025-06-20 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1258', '2025-06-20 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1259', '2025-06-20 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1260', '2025-06-20 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1263', '2025-06-20 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1264', '2025-06-20 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1265', '2025-06-20 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1266', '2025-06-20 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1271', '2025-06-21 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1272', '2025-06-21 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1275', '2025-06-21 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1276', '2025-06-21 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1279', '2025-07-03 15:57:50:44', '123', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('1280', '2025-07-03 15:57:40:101', '123', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1281', '2025-06-21 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1282', '2025-06-21 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1283', '2025-06-21 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1284', '2025-06-21 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1287', '2025-06-21 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1288', '2025-06-21 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1289', '2025-06-21 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1290', '2025-06-21 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1295', '2025-07-01 08:30:00:00', '136', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('1296', '2025-07-01 17:30:00:00', '136', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1299', '2025-07-01 08:30:00:00', '121', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('1300', '2025-07-01 17:30:00:00', '121', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1303', '2025-07-01 08:30:00:00', '123', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('1304', '2025-07-01 17:30:00:00', '123', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1305', '2025-07-01 08:30:00:00', '127', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('1306', '2025-07-01 17:30:00:00', '127', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1307', '2025-07-01 08:30:00:00', '129', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('1308', '2025-07-01 17:30:00:00', '129', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1311', '2025-07-01 08:30:00:00', '128', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('1312', '2025-07-01 17:30:00:00', '128', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1313', '2025-07-01 08:30:00:00', '134', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('1314', '2025-07-01 17:30:00:00', '134', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1321', '2025-07-06 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1322', '2025-07-06 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1325', '2025-07-06 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1326', '2025-07-06 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1329', '2025-07-06 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1330', '2025-07-06 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1331', '2025-07-06 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1332', '2025-07-06 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1333', '2025-07-06 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1334', '2025-07-06 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1337', '2025-07-06 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1338', '2025-07-06 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1339', '2025-07-06 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1340', '2025-07-06 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1343', '2025-07-06 08:30:00:00', '147', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1344', '2025-07-06 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1349', '2025-07-07 16:56:07:800', '136', '已签到', 'a', '位置解析失败');
INSERT INTO `sign` VALUES ('1350', '2025-07-07 16:57:05:492', '136', '已签到', 'p', '福建省福州市马尾区船政路');
INSERT INTO `sign` VALUES ('1353', '2025-07-07 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1354', '2025-07-07 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1357', '2025-07-07 16:11:25:10', '123', '已签到', 'a', '福建省福州市马尾区');
INSERT INTO `sign` VALUES ('1358', '2025-07-07 16:12:23:970', '123', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1359', '2025-07-07 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1360', '2025-07-07 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1361', '2025-07-07 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1362', '2025-07-07 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1365', '2025-07-07 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1366', '2025-07-07 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1367', '2025-07-07 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1368', '2025-07-07 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1371', '2025-07-07 16:21:37:909', '147', '已签到', 'a', '福建省福州市马尾区船政路');
INSERT INTO `sign` VALUES ('1372', '2025-07-07 16:21:56:603', '147', '已签到', 'p', '福建省福州市马尾区船政路');
INSERT INTO `sign` VALUES ('1377', '2025-07-08 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1378', '2025-07-08 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1381', '2025-07-08 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1382', '2025-07-08 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1385', '2025-07-08 15:42:51:826', '123', '已签到', 'a', '福建省福州市马尾区船政路');
INSERT INTO `sign` VALUES ('1386', '2025-07-08 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1387', '2025-07-08 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1388', '2025-07-08 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1389', '2025-07-08 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1390', '2025-07-08 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1393', '2025-07-08 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1394', '2025-07-08 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1395', '2025-07-08 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1396', '2025-07-08 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1399', '2025-07-08 08:30:00:00', '147', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1400', '2025-07-08 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1405', '2025-07-09 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1406', '2025-07-09 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1409', '2025-07-09 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1410', '2025-07-09 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1413', '2025-07-09 09:33:26:865', '123', '已签到', 'a', '福建省福州市马尾区快洲路');
INSERT INTO `sign` VALUES ('1414', '2025-07-09 11:01:08:357', '123', '已签到', 'p', '福建省福州市马尾区船政路');
INSERT INTO `sign` VALUES ('1415', '2025-07-09 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1416', '2025-07-09 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1417', '2025-07-09 10:33:12:10', '129', '已签到', 'a', '福建省福州市马尾区船政路');
INSERT INTO `sign` VALUES ('1418', '2025-07-09 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1421', '2025-07-09 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1422', '2025-07-09 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1423', '2025-07-09 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1424', '2025-07-09 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1427', '2025-07-09 08:30:00:00', '147', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1428', '2025-07-09 11:18:43:103', '147', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1433', '2025-07-11 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1434', '2025-07-11 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1437', '2025-07-11 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1438', '2025-07-11 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1441', '2025-07-11 15:37:46:321', '123', '未签到', 'a', '福建省福州市马尾区船政路');
INSERT INTO `sign` VALUES ('1442', '2025-07-11 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1443', '2025-07-11 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1444', '2025-07-11 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1445', '2025-07-11 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1446', '2025-07-11 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1449', '2025-07-11 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1450', '2025-07-11 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1451', '2025-07-11 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1452', '2025-07-11 15:38:36:540', '127', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1453', '2025-07-11 08:30:00:00', '147', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1454', '2025-07-11 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1459', '2025-07-12 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1460', '2025-07-12 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1463', '2025-07-12 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1464', '2025-07-12 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1467', '2025-07-12 15:22:18:369', '123', '已签到', 'a', '福建省福州市马尾区船政路');
INSERT INTO `sign` VALUES ('1468', '2025-07-12 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1469', '2025-07-12 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1470', '2025-07-12 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1471', '2025-07-12 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1472', '2025-07-12 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1475', '2025-07-12 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1476', '2025-07-12 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1477', '2025-07-12 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1478', '2025-07-12 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1479', '2025-07-12 08:30:00:00', '147', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1480', '2025-07-12 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1485', '2025-12-08 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1486', '2025-12-08 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1489', '2025-12-08 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1490', '2025-12-08 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1493', '2025-12-08 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1494', '2025-12-08 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1495', '2025-12-08 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1496', '2025-12-08 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1497', '2025-12-08 14:42:06:914', '129', '未签到', 'a', '福建省福州市马尾区上坂路');
INSERT INTO `sign` VALUES ('1498', '2025-12-08 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1501', '2025-12-08 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1502', '2025-12-08 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1503', '2025-12-08 09:25:04:283', '127', '已签到', 'a', '福建省福州市马尾区铁南路');
INSERT INTO `sign` VALUES ('1504', '2025-12-08 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1505', '2025-12-08 09:31:23:249', '147', '已签到', 'a', '福建省福州市马尾区铁南路');
INSERT INTO `sign` VALUES ('1506', '2025-12-08 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1537', '2025-12-15 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1538', '2025-12-15 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1541', '2025-12-15 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1542', '2025-12-15 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1545', '2025-12-15 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1546', '2025-12-15 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1547', '2025-12-15 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1548', '2025-12-15 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1551', '2025-12-15 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1552', '2025-12-15 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1553', '2025-12-15 08:30:00:00', '147', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1554', '2025-12-15 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1555', '2025-12-15 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1556', '2025-12-15 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1557', '2025-12-15 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1558', '2025-12-15 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1563', '2025-12-16 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1564', '2025-12-16 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1567', '2025-12-16 10:07:50:401', '121', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('1568', '2025-12-16 10:17:31:544', '121', '已签到', 'p', '福建省福州市马尾区上坂路');
INSERT INTO `sign` VALUES ('1571', '2025-12-16 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1572', '2025-12-16 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1573', '2025-12-16 10:22:25:754', '128', '已签到', 'a', '福建省福州市马尾区上坂路');
INSERT INTO `sign` VALUES ('1574', '2025-12-16 10:25:00:308', '128', '已签到', 'p', '位置解析失败');
INSERT INTO `sign` VALUES ('1577', '2025-12-16 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1578', '2025-12-16 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1579', '2025-12-16 09:07:04:856', '147', '已签到', 'a', '福建省福州市马尾区铁南路');
INSERT INTO `sign` VALUES ('1580', '2025-12-16 10:13:35:218', '147', '已签到', 'p', null);
INSERT INTO `sign` VALUES ('1581', '2025-12-16 10:05:10:160', '127', '已签到', 'a', null);
INSERT INTO `sign` VALUES ('1582', '2025-12-16 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1583', '2025-12-16 10:21:41:733', '129', '已签到', 'a', '福建省福州市马尾区上坂路');
INSERT INTO `sign` VALUES ('1584', '2025-12-16 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1589', '2025-12-17 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1590', '2025-12-17 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1593', '2025-12-17 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1594', '2025-12-17 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1597', '2025-12-17 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1598', '2025-12-17 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1599', '2025-12-17 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1600', '2025-12-17 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1603', '2025-12-17 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1604', '2025-12-17 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1605', '2025-12-17 09:08:52:827', '147', '已签到', 'a', '福建省福州市马尾区铁南路');
INSERT INTO `sign` VALUES ('1606', '2025-12-17 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1607', '2025-12-17 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1608', '2025-12-17 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1609', '2025-12-17 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1610', '2025-12-17 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1611', '2025-12-22 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1612', '2025-12-22 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1613', '2025-12-22 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1614', '2025-12-22 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1617', '2025-12-22 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1618', '2025-12-22 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1619', '2025-12-22 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1620', '2025-12-22 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1621', '2025-12-22 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1622', '2025-12-22 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1623', '2025-12-22 16:37:09:588', '147', '已签到', 'a', '福建省福州市马尾区上坂路');
INSERT INTO `sign` VALUES ('1624', '2025-12-22 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1627', '2025-12-22 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1628', '2025-12-22 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1629', '2025-12-22 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1630', '2025-12-22 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1631', '2025-12-22 08:30:00:00', '148', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1632', '2025-12-22 17:30:00:00', '148', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1633', '2025-12-22 08:30:00:00', '149', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1634', '2025-12-22 17:30:00:00', '149', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1635', '2025-12-23 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1636', '2025-12-23 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1637', '2025-12-23 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1638', '2025-12-23 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1641', '2025-12-23 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1642', '2025-12-23 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1643', '2025-12-23 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1644', '2025-12-23 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1645', '2025-12-23 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1646', '2025-12-23 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1647', '2025-12-23 09:10:11:268', '147', '已签到', 'a', '福建省福州市马尾区上坂路');
INSERT INTO `sign` VALUES ('1648', '2025-12-23 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1651', '2025-12-23 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1652', '2025-12-23 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1653', '2025-12-23 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1654', '2025-12-23 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1655', '2025-12-23 08:30:00:00', '148', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1656', '2025-12-23 17:30:00:00', '148', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1657', '2025-12-23 08:30:00:00', '149', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1658', '2025-12-23 17:30:00:00', '149', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1659', '2025-12-23 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1660', '2025-12-23 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1661', '2025-12-23 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1662', '2025-12-23 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1665', '2025-12-23 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1666', '2025-12-23 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1667', '2025-12-23 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1668', '2025-12-23 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1669', '2025-12-23 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1670', '2025-12-23 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1671', '2025-12-23 08:30:00:00', '147', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1672', '2025-12-23 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1675', '2025-12-23 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1676', '2025-12-23 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1679', '2025-12-23 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1680', '2025-12-23 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1681', '2025-12-23 08:30:00:00', '148', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1682', '2025-12-23 17:30:00:00', '148', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1683', '2025-12-23 08:30:00:00', '149', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1684', '2025-12-23 17:30:00:00', '149', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1685', '2025-12-23 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1686', '2025-12-23 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1687', '2025-12-23 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1688', '2025-12-23 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1691', '2025-12-23 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1692', '2025-12-23 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1693', '2025-12-23 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1694', '2025-12-23 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1695', '2025-12-23 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1696', '2025-12-23 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1697', '2025-12-23 08:30:00:00', '147', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1698', '2025-12-23 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1701', '2025-12-23 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1702', '2025-12-23 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1705', '2025-12-23 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1706', '2025-12-23 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1709', '2025-12-23 08:30:00:00', '148', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1710', '2025-12-23 17:30:00:00', '148', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1711', '2025-12-23 08:30:00:00', '149', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1712', '2025-12-23 17:30:00:00', '149', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1713', '2025-12-24 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1714', '2025-12-24 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1715', '2025-12-24 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1716', '2025-12-24 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1719', '2025-12-24 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1720', '2025-12-24 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1721', '2025-12-24 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1722', '2025-12-24 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1723', '2025-12-24 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1724', '2025-12-24 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1725', '2025-12-24 08:30:00:00', '147', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1726', '2025-12-24 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1729', '2025-12-24 09:17:34:910', '153', '已签到', 'a', '福建省福州市马尾区铁南路');
INSERT INTO `sign` VALUES ('1730', '2025-12-24 17:30:00:00', '153', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1731', '2025-12-24 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1732', '2025-12-24 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1735', '2025-12-24 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1736', '2025-12-24 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1739', '2025-12-24 08:30:00:00', '148', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1740', '2025-12-24 17:30:00:00', '148', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1741', '2025-12-24 08:30:00:00', '149', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1742', '2025-12-24 17:30:00:00', '149', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1743', '2025-12-25 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1744', '2025-12-25 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1745', '2025-12-25 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1746', '2025-12-25 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1749', '2025-12-25 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1750', '2025-12-25 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1751', '2025-12-25 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1752', '2025-12-25 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1753', '2025-12-25 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1754', '2025-12-25 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1755', '2025-12-25 14:21:10:136', '147', '已签到', 'a', '福建省福州市马尾区铁南路');
INSERT INTO `sign` VALUES ('1756', '2025-12-25 16:45:01:767', '147', '未签到', 'p', '福建省福州市马尾区铁南路');
INSERT INTO `sign` VALUES ('1757', '2025-12-25 16:44:43:875', '153', '未签到', 'a', '福建省福州市马尾区铁南路');
INSERT INTO `sign` VALUES ('1758', '2025-12-25 17:30:00:00', '153', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1759', '2025-12-25 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1760', '2025-12-25 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1761', '2025-12-25 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1762', '2025-12-25 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1763', '2025-12-25 08:30:00:00', '148', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1764', '2025-12-25 17:30:00:00', '148', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1765', '2025-12-25 08:30:00:00', '149', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1766', '2025-12-25 17:30:00:00', '149', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1767', '2025-12-31 08:30:00:00', '149', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1768', '2025-12-31 17:30:00:00', '149', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1769', '2025-12-31 09:52:35:19', '136', '已签到', 'a', '福建省福州市马尾区上坂路');
INSERT INTO `sign` VALUES ('1770', '2025-12-31 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1771', '2025-12-31 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1772', '2025-12-31 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1775', '2025-12-31 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1776', '2025-12-31 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1777', '2025-12-31 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1778', '2025-12-31 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1779', '2025-12-31 09:52:51:592', '134', '已签到', 'a', '福建省福州市马尾区上坂路');
INSERT INTO `sign` VALUES ('1780', '2025-12-31 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1781', '2025-12-31 09:50:43:274', '147', '已签到', 'a', '福建省福州市马尾区儒江西路126号');
INSERT INTO `sign` VALUES ('1782', '2025-12-31 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1783', '2025-12-31 08:30:00:00', '153', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1784', '2025-12-31 17:30:00:00', '153', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1785', '2025-12-31 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1786', '2025-12-31 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1787', '2025-12-31 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1788', '2025-12-31 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1789', '2025-12-31 08:30:00:00', '148', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1790', '2025-12-31 17:30:00:00', '148', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1791', '2026-01-26 08:30:00:00', '149', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1792', '2026-01-26 17:30:00:00', '149', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1793', '2026-01-26 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1794', '2026-01-26 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1795', '2026-01-26 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1796', '2026-01-26 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1799', '2026-01-26 08:30:00:00', '123', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1800', '2026-01-26 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1801', '2026-01-26 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1802', '2026-01-26 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1803', '2026-01-26 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1804', '2026-01-26 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1805', '2026-01-26 08:30:00:00', '147', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1806', '2026-01-26 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1807', '2026-01-26 08:30:00:00', '153', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1808', '2026-01-26 17:30:00:00', '153', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1809', '2026-01-26 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1810', '2026-01-26 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1811', '2026-01-26 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1812', '2026-01-26 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1813', '2026-01-26 08:30:00:00', '148', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1814', '2026-01-26 17:30:00:00', '148', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1815', '2026-01-27 08:30:00:00', '149', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1816', '2026-01-27 17:30:00:00', '149', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1817', '2026-01-27 08:30:00:00', '136', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1818', '2026-01-27 17:30:00:00', '136', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1819', '2026-01-27 08:30:00:00', '121', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1820', '2026-01-27 17:30:00:00', '121', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1821', '2026-01-27 11:31:46:382', '123', '已签到', 'a', '福建省福州市马尾区上坂路');
INSERT INTO `sign` VALUES ('1822', '2026-01-27 17:30:00:00', '123', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1823', '2026-01-27 08:30:00:00', '128', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1824', '2026-01-27 17:30:00:00', '128', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1825', '2026-01-27 08:30:00:00', '134', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1826', '2026-01-27 17:30:00:00', '134', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1827', '2026-01-27 11:31:13:924', '147', '已签到', 'a', '福建省福州市马尾区上坂路');
INSERT INTO `sign` VALUES ('1828', '2026-01-27 17:30:00:00', '147', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1829', '2026-01-27 08:30:00:00', '153', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1830', '2026-01-27 17:30:00:00', '153', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1831', '2026-01-27 08:30:00:00', '127', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1832', '2026-01-27 17:30:00:00', '127', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1833', '2026-01-27 08:30:00:00', '129', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1834', '2026-01-27 17:30:00:00', '129', '未签到', 'p', null);
INSERT INTO `sign` VALUES ('1835', '2026-01-27 08:30:00:00', '148', '未签到', 'a', null);
INSERT INTO `sign` VALUES ('1836', '2026-01-27 17:30:00:00', '148', '未签到', 'p', null);
