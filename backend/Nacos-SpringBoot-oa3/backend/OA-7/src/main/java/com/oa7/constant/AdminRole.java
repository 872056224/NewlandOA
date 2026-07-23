package com.oa7.constant;

/**
 * 管理员角色枚举
 * 基于员工的组织架构职务动态计算
 */
public enum AdminRole {

    /** 董事长 — 最高全局权限 */
    CHAIRMAN,

    /** 人事部部长 — 全局业务权限 + 受限的人事权 */
    HR_DIRECTOR,

    /** 部门部长/副部长 — 本部门数据隔离权限 */
    DEPT_HEAD
}
