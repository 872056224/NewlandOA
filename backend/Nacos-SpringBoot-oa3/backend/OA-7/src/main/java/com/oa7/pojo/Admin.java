package com.oa7.pojo;

import com.oa7.constant.AdminRole;
import lombok.Data;

/**
 * @name: chenle
 * @Date: 2021/11/30 20:11
 * @Author: IAO
 * @Description: ...
 */
@Data
public class Admin {
    private int id;
    private String name;
    private String pwd;

    // ===== RBAC 扩展字段 =====

    /** 关联员工编号（admin → emp 的绑定） */
    private Integer empNumber;

    // ===== 非持久化字段（登录时联表查询填充） =====

    /** 员工姓名 */
    private String empName;
    /** 员工所在部门ID */
    private Integer deptId;
    /** 员工职务ID */
    private Integer dutyId;
    /** 部门名称 */
    private String deptName;
    /** 计算后的角色 */
    private AdminRole role;

    /**
     * 判断是否有指定的角色
     */
    public boolean hasRole(AdminRole targetRole) {
        return this.role == targetRole;
    }

    /**
     * 判断是否为部门部长/副部长（数据隔离）
     */
    public boolean isDeptHead() {
        return this.role == AdminRole.DEPT_HEAD;
    }

    /**
     * 判断是否有全局访问权限
     */
    public boolean hasGlobalAccess() {
        return this.role == AdminRole.CHAIRMAN || this.role == AdminRole.HR_DIRECTOR;
    }
}
