-- ============================================================
-- OA 系统 RBAC 数据填充脚本 (MySQL 5.7 兼容)
-- ============================================================

-- ===================== 1. 结构变更 =====================

-- 添加 emp_number 字段（如已存在会报错，忽略即可）
ALTER TABLE `admin`
  ADD COLUMN `emp_number` INT(11) DEFAULT NULL COMMENT '关联员工编号',
  ADD INDEX `idx_emp_number` (`emp_number`);

-- duty 表补充数据
INSERT IGNORE INTO `duty` (`duty_id`, `duty_name`) VALUES (2, '副部长');
INSERT IGNORE INTO `duty` (`duty_id`, `duty_name`) VALUES (17, '董事长');

-- ===================== 2. 清空旧数据 =====================

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM `makeup_request`;
DELETE FROM `retroactive_sign`;
DELETE FROM `notification`;
DELETE FROM `leave`;
DELETE FROM `attendance`;
DELETE FROM `sign`;
DELETE FROM `emp`;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE `emp` AUTO_INCREMENT = 155;

-- ===================== 3. 插入员工 =====================

-- MD5('123') = 202cb962ac59075b964b07152d234b70
SET @PWD = '202cb962ac59075b964b07152d234b70';

-- 董事长 (duty_id=17)
INSERT INTO `emp` (`number`, `name`, `pwd`, `birthday`, `address`, `dept_id`, `duty_id`)
VALUES (155, '陈乐', @PWD, '1990-01-01', '福建省福州市', 3, 17);

-- 人事部 (dept_id=1)
INSERT INTO `emp` VALUES (156, '荀彧', @PWD, '1991-01-01', '福建省福州市', 1, 1);
INSERT INTO `emp` VALUES (157, '荀攸', @PWD, '1992-02-01', '福建省福州市', 1, 2);
INSERT INTO `emp` VALUES (158, '郭嘉', @PWD, '1993-03-01', '福建省福州市', 1, 9);
INSERT INTO `emp` VALUES (159, '程昱', @PWD, '1994-04-01', '福建省福州市', 1, 9);
INSERT INTO `emp` VALUES (160, '贾诩', @PWD, '1995-05-01', '福建省福州市', 1, 9);

-- 后勤部 (dept_id=2)
INSERT INTO `emp` VALUES (161, '张飞', @PWD, '1991-01-01', '福建省福州市', 2, 1);
INSERT INTO `emp` VALUES (162, '赵云', @PWD, '1992-02-01', '福建省福州市', 2, 2);
INSERT INTO `emp` VALUES (163, '廖化', @PWD, '1993-03-01', '福建省福州市', 2, 9);
INSERT INTO `emp` VALUES (164, '周仓', @PWD, '1994-04-01', '福建省福州市', 2, 9);
INSERT INTO `emp` VALUES (165, '关平', @PWD, '1995-05-01', '福建省福州市', 2, 9);

-- Java研发部 (dept_id=3)
INSERT INTO `emp` VALUES (166, '周瑜', @PWD, '1991-01-01', '福建省福州市', 3, 1);
INSERT INTO `emp` VALUES (167, '陆逊', @PWD, '1992-02-01', '福建省福州市', 3, 2);
INSERT INTO `emp` VALUES (168, '吕蒙', @PWD, '1993-03-01', '福建省福州市', 3, 9);
INSERT INTO `emp` VALUES (169, '甘宁', @PWD, '1994-04-01', '福建省福州市', 3, 9);
INSERT INTO `emp` VALUES (170, '黄盖', @PWD, '1995-05-01', '福建省福州市', 3, 9);

-- 商品部 (dept_id=4)
INSERT INTO `emp` VALUES (171, '关羽', @PWD, '1991-01-01', '福建省福州市', 4, 1);
INSERT INTO `emp` VALUES (172, '张辽', @PWD, '1992-02-01', '福建省福州市', 4, 2);
INSERT INTO `emp` VALUES (173, '徐晃', @PWD, '1993-03-01', '福建省福州市', 4, 9);
INSERT INTO `emp` VALUES (174, '于禁', @PWD, '1994-04-01', '福建省福州市', 4, 9);
INSERT INTO `emp` VALUES (175, '乐进', @PWD, '1995-05-01', '福建省福州市', 4, 9);

-- 销售部 (dept_id=5)
INSERT INTO `emp` VALUES (176, '吕布', @PWD, '1991-01-01', '福建省福州市', 5, 1);
INSERT INTO `emp` VALUES (177, '高顺', @PWD, '1992-02-01', '福建省福州市', 5, 2);
INSERT INTO `emp` VALUES (178, '张郃', @PWD, '1993-03-01', '福建省福州市', 5, 9);
INSERT INTO `emp` VALUES (179, '夏侯渊', @PWD, '1994-04-01', '福建省福州市', 5, 9);
INSERT INTO `emp` VALUES (180, '曹仁', @PWD, '1995-05-01', '福建省福州市', 5, 9);

-- 行政部 (dept_id=6)
INSERT INTO `emp` VALUES (181, '诸葛亮', @PWD, '1991-01-01', '福建省福州市', 6, 1);
INSERT INTO `emp` VALUES (182, '庞统', @PWD, '1992-02-01', '福建省福州市', 6, 2);
INSERT INTO `emp` VALUES (183, '马谡', @PWD, '1993-03-01', '福建省福州市', 6, 9);
INSERT INTO `emp` VALUES (184, '姜维', @PWD, '1994-04-01', '福建省福州市', 6, 9);
INSERT INTO `emp` VALUES (185, '法正', @PWD, '1995-05-01', '福建省福州市', 6, 9);

-- 大数据研发部 (dept_id=17)
INSERT INTO `emp` VALUES (186, '司马懿', @PWD, '1991-01-01', '福建省福州市', 17, 1);
INSERT INTO `emp` VALUES (187, '邓艾', @PWD, '1992-02-01', '福建省福州市', 17, 2);
INSERT INTO `emp` VALUES (188, '钟会', @PWD, '1993-03-01', '福建省福州市', 17, 9);
INSERT INTO `emp` VALUES (189, '陈泰', @PWD, '1994-04-01', '福建省福州市', 17, 9);
INSERT INTO `emp` VALUES (190, '郭淮', @PWD, '1995-05-01', '福建省福州市', 17, 9);

-- 前端研发部 (dept_id=18)
INSERT INTO `emp` VALUES (191, '孙策', @PWD, '1991-01-01', '福建省福州市', 18, 1);
INSERT INTO `emp` VALUES (192, '孙权', @PWD, '1992-02-01', '福建省福州市', 18, 2);
INSERT INTO `emp` VALUES (193, '周泰', @PWD, '1993-03-01', '福建省福州市', 18, 9);
INSERT INTO `emp` VALUES (194, '蒋钦', @PWD, '1994-04-01', '福建省福州市', 18, 9);
INSERT INTO `emp` VALUES (195, '凌统', @PWD, '1995-05-01', '福建省福州市', 18, 9);

-- 测试部 (dept_id=19)
INSERT INTO `emp` VALUES (196, '典韦', @PWD, '1991-01-01', '福建省福州市', 19, 1);
INSERT INTO `emp` VALUES (197, '许褚', @PWD, '1992-02-01', '福建省福州市', 19, 2);
INSERT INTO `emp` VALUES (198, '曹洪', @PWD, '1993-03-01', '福建省福州市', 19, 9);
INSERT INTO `emp` VALUES (199, '曹彰', @PWD, '1994-04-01', '福建省福州市', 19, 9);
INSERT INTO `emp` VALUES (200, '曹真', @PWD, '1995-05-01', '福建省福州市', 19, 9);

-- ===================== 4. 管理员绑定 =====================

-- 清空所有 admin 的 emp_number
UPDATE `admin` SET `emp_number` = NULL;

-- 绑定 chenle → 董事长 (emp 155)
UPDATE `admin` SET `emp_number` = 155 WHERE `name` = 'chenle';

-- ===================== 5. 新建管理员账号 =====================

INSERT INTO `admin` (`name`, `pwd`, `emp_number`) VALUES
-- 人事部
('hr_xunyu',   @PWD, 156),
('hr_xunyou',  @PWD, 157),
-- 后勤部
('hq_zhangfei', @PWD, 161),
('hq_zhaoyun',  @PWD, 162),
-- Java研发部
('java_zhouyu', @PWD, 166),
('java_luxun',  @PWD, 167),
-- 商品部
('goods_guanyu',  @PWD, 171),
('goods_zhangliao', @PWD, 172),
-- 销售部
('sales_lvbu',    @PWD, 176),
('sales_gaoshun', @PWD, 177),
-- 行政部
('admin_zhugeliang', @PWD, 181),
('admin_pangtong',   @PWD, 182),
-- 大数据研发部
('bigdata_simayi', @PWD, 186),
('bigdata_dengai', @PWD, 187),
-- 前端研发部
('front_sunce',  @PWD, 191),
('front_sunquan', @PWD, 192),
-- 测试部
('test_dianwei', @PWD, 196),
('test_xuchu',   @PWD, 197);

-- ===================== 6. 生成7月考勤记录 =====================

INSERT INTO `attendance` (`emp_id`, `date`, `check_in_time`, `check_out_time`, `today_status`, `attendance_status`)
SELECT e.n, d.dt,
       CONCAT(d.dt, ' 09:00:00'),
       CONCAT(d.dt, ' 18:00:00'),
       'CHECKED_OUT', 'NORMAL'
FROM (
    SELECT 155 AS n UNION SELECT 156 UNION SELECT 157 UNION SELECT 158 UNION SELECT 159
    UNION SELECT 160 UNION SELECT 161 UNION SELECT 162 UNION SELECT 163 UNION SELECT 164
    UNION SELECT 165 UNION SELECT 166 UNION SELECT 167 UNION SELECT 168 UNION SELECT 169
    UNION SELECT 170 UNION SELECT 171 UNION SELECT 172 UNION SELECT 173 UNION SELECT 174
    UNION SELECT 175 UNION SELECT 176 UNION SELECT 177 UNION SELECT 178 UNION SELECT 179
    UNION SELECT 180 UNION SELECT 181 UNION SELECT 182 UNION SELECT 183 UNION SELECT 184
    UNION SELECT 185 UNION SELECT 186 UNION SELECT 187 UNION SELECT 188 UNION SELECT 189
    UNION SELECT 190 UNION SELECT 191 UNION SELECT 192 UNION SELECT 193 UNION SELECT 194
    UNION SELECT 195 UNION SELECT 196 UNION SELECT 197 UNION SELECT 198 UNION SELECT 199
    UNION SELECT 200
) e
CROSS JOIN (
    SELECT '2026-07-01' AS dt UNION SELECT '2026-07-02' UNION SELECT '2026-07-03'
    UNION SELECT '2026-07-06' UNION SELECT '2026-07-07' UNION SELECT '2026-07-08'
    UNION SELECT '2026-07-09' UNION SELECT '2026-07-10'
    UNION SELECT '2026-07-13' UNION SELECT '2026-07-14' UNION SELECT '2026-07-15'
    UNION SELECT '2026-07-16' UNION SELECT '2026-07-17'
    UNION SELECT '2026-07-20' UNION SELECT '2026-07-21' UNION SELECT '2026-07-22'
    UNION SELECT '2026-07-23'
) d
WHERE NOT EXISTS (
    SELECT 1 FROM `attendance` a WHERE a.emp_id = e.n AND a.date = d.dt
);

-- ===================== 7. 验证 =====================

SELECT '=== 员工统计 ===' AS status;
SELECT d.dept_name, COUNT(*) AS emp_count
FROM `emp` e
JOIN `department` d ON d.dept_id = e.dept_id
GROUP BY d.dept_id, d.dept_name
ORDER BY d.dept_id;

SELECT '=== 管理员统计 ===' AS status;
SELECT a.`name` AS admin_name, e.`name` AS emp_name,
       du.duty_name, dp.dept_name,
       CASE
           WHEN e.duty_id = 17 THEN 'CHAIRMAN'
           WHEN e.dept_id = 1 AND e.duty_id = 1 THEN 'HR_DIRECTOR'
           WHEN e.duty_id IN (1,2) THEN 'DEPT_HEAD'
           ELSE 'NONE'
       END AS role
FROM `admin` a
LEFT JOIN `emp` e ON e.number = a.emp_number
LEFT JOIN `duty` du ON du.duty_id = e.duty_id
LEFT JOIN `department` dp ON dp.dept_id = e.dept_id
WHERE a.emp_number IS NOT NULL
ORDER BY e.duty_id, e.dept_id;

SELECT '=== 7月考勤记录数 ===' AS status;
SELECT COUNT(*) AS attendance_count FROM `attendance`
WHERE `date` >= '2026-07-01' AND `date` <= '2026-07-23';
