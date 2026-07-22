package com.oa7.service;

import com.oa7.pojo.AttendanceRule;
import com.oa7.util.RESP;

public interface AttendanceRuleService {

    /**
     * 获取全局默认规则
     */
    RESP getDefault();

    /**
     * 获取指定部门的规则
     */
    RESP getByDept(int deptId);

    /**
     * 获取所有规则
     */
    RESP getAll();

    /**
     * 新增或更新规则
     */
    RESP save(AttendanceRule rule);

    /**
     * 软删除规则（设置 enabled = false）
     */
    RESP delete(int id);
}
