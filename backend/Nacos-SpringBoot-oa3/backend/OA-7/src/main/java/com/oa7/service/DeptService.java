package com.oa7.service;

import com.oa7.pojo.Department;
import com.oa7.util.RESP;

/**
 * @name: chenle
 * @Date: 2021/12/3 14:43
 * @Author: oa5
 * @Description: 部门管理服务接口
 */
public interface DeptService {

    /**
     * 分页查询部门列表（带Redis缓存）
     */
    RESP selectByPage(int currentPage, int pageSize);

    /**
     * 添加部门（带Redis延迟双删）
     */
    RESP add(Department dept, int currentPage, int pageSize);

    /**
     * 更新部门名称（带Redis延迟双删）
     */
    RESP update(int deptId, Department dept, int currentPage, int pageSize);
}
