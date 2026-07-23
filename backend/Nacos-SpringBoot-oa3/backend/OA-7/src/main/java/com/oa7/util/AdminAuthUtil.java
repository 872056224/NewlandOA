package com.oa7.util;

import com.oa7.constant.AdminRole;
import com.oa7.pojo.Admin;

import javax.servlet.http.HttpSession;

/**
 * 管理员鉴权工具类
 * 负责角色计算、权限校验
 */
public class AdminAuthUtil {

    /**
     * 根据职务和部门计算角色
     * @param deptId 部门ID
     * @param dutyId 职务ID
     * @return 计算出的角色，如果不是可登录管理端的职务则返回null
     */
    public static AdminRole computeRole(int deptId, int dutyId) {
        // 董事长 (duty_id=17)
        if (dutyId == 17) {
            return AdminRole.CHAIRMAN;
        }
        // 人事部部长 (dept_id=1, duty_id=1)
        if (deptId == 1 && dutyId == 1) {
            return AdminRole.HR_DIRECTOR;
        }
        // 部门部长/副部长 (duty_id=1 或 2)
        if (dutyId == 1 || dutyId == 2) {
            return AdminRole.DEPT_HEAD;
        }
        // 普通员工 → 无管理端权限
        return null;
    }

    /**
     * 从 Session 获取当前管理员
     */
    public static Admin getCurrentAdmin(HttpSession session) {
        if (session == null) return null;
        return (Admin) session.getAttribute("admin");
    }

    /**
     * 从 Session 获取当前角色
     */
    public static AdminRole getCurrentRole(HttpSession session) {
        Admin admin = getCurrentAdmin(session);
        return admin != null ? admin.getRole() : null;
    }

    /**
     * 从 Session 获取当前管理员的部门ID
     */
    public static Integer getCurrentDeptId(HttpSession session) {
        Admin admin = getCurrentAdmin(session);
        return admin != null ? admin.getDeptId() : null;
    }

    /**
     * 从 Session 获取当前管理员的部门名称
     */
    public static String getCurrentDeptName(HttpSession session) {
        Admin admin = getCurrentAdmin(session);
        return admin != null ? admin.getDeptName() : null;
    }

    /**
     * 判断当前角色是否为 DEPT_HEAD
     */
    public static boolean isDeptHead(HttpSession session) {
        return AdminRole.DEPT_HEAD == getCurrentRole(session);
    }

    /**
     * 判断当前角色是否有全局访问权限（董事长或人事部部长）
     */
    public static boolean hasGlobalAccess(HttpSession session) {
        AdminRole role = getCurrentRole(session);
        return role == AdminRole.CHAIRMAN || role == AdminRole.HR_DIRECTOR;
    }

    /**
     * 判断是否有权修改该员工（基于当前职务）
     * @return null 表示允许，非null为错误消息
     */
    public static String checkEmpModifyPermission(Admin currentAdmin, int targetDeptId, int targetDutyId) {
        return checkEmpModifyPermission(currentAdmin, targetDeptId, targetDutyId, null);
    }

    /**
     * 判断是否有权修改该员工（含新职务校验）
     * @param currentAdmin 当前操作的管理员
     * @param targetDeptId 目标员工当前部门
     * @param targetDutyId 目标员工当前职务
     * @param newDutyId 目标员工将要被设置的新职务（null表示不检查新值）
     * @return null 表示允许，非null为错误消息
     */
    public static String checkEmpModifyPermission(Admin currentAdmin, int targetDeptId, int targetDutyId, Integer newDutyId) {
        if (currentAdmin == null) return "未登录";

        AdminRole role = currentAdmin.getRole();
        if (role == null) return "无权限";

        // 董事长：允许所有
        if (role == AdminRole.CHAIRMAN) {
            return null;
        }

        // 人事部部长：不能操作董事长(duty_id=17)和部长(duty_id=1)
        if (role == AdminRole.HR_DIRECTOR) {
            if (targetDutyId == 17) {
                return "无权操作董事长";
            }
            if (targetDutyId == 1) {
                return "无权操作各部门部长";
            }
            // 也不能把任何人设置为董事长或部长
            if (newDutyId != null && (newDutyId == 17 || newDutyId == 1)) {
                return "无权将任何人设置为部长及以上职务";
            }
            return null;
        }

        // 部门部长/副部长：只能操作本部门，且不能修改部长/董事长
        if (role == AdminRole.DEPT_HEAD) {
            if (!currentAdmin.getDeptId().equals(targetDeptId)) {
                return "只能操作本部门员工";
            }
            if (targetDutyId == 17 || targetDutyId == 1) {
                return "无权操作部长及以上职务";
            }
            // 也不能把任何人设置为部长/董事长
            if (newDutyId != null && (newDutyId == 17 || newDutyId == 1)) {
                return "无权将任何人设置为部长及以上职务";
            }
            return null;
        }

        return "无权限";
    }

    /**
     * 判断是否有权新增员工（基于目标职务）
     * @return null 表示允许，非null为错误消息
     */
    public static String checkEmpAddPermission(Admin currentAdmin, int newDutyId) {
        if (currentAdmin == null) return "未登录";
        AdminRole role = currentAdmin.getRole();
        if (role == null) return "无权限";

        // 董事长允许所有
        if (role == AdminRole.CHAIRMAN) {
            return null;
        }

        // 人事部部长不能新增董事长和部长
        if (role == AdminRole.HR_DIRECTOR) {
            if (newDutyId == 17 || newDutyId == 1) {
                return "无权新增部长及以上职务的员工";
            }
            return null;
        }

        // 部门部长/副部长只能在本部门新增普通员工
        if (role == AdminRole.DEPT_HEAD) {
            if (newDutyId == 17 || newDutyId == 1 || newDutyId == 2) {
                return "无权新增部长/副部长";
            }
            return null;
        }

        return "无权限";
    }
}
