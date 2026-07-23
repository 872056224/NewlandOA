package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.constant.AdminRole;
import com.oa7.pojo.Admin;
import com.oa7.pojo.Department;
import com.oa7.service.EmpService;
import com.oa7.dao.EmpDao;
import com.oa7.pojo.Emp;
import com.oa7.util.AdminAuthUtil;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * @name: chenle
 * @Date: 2021/12/3 14:57
 * @Author: IAO
 * @Description: ...
 */
@Service
public class EmpServiceImpl implements EmpService {

    @Autowired
    private EmpDao empDao;

    @Override
    public RESP selectByPage(int currentPage, int pageSize, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);

        PageHelper.startPage(currentPage, pageSize);
        List<Emp> list;

        if (admin != null && admin.isDeptHead()) {
            // DEPT_HEAD：只看到本部门员工
            list = empDao.selectByPageHelperByDept(admin.getDeptId());
        } else {
            // CHAIRMAN / HR_DIRECTOR：看到全部
            list = empDao.selectByPageHelper();
        }

        PageInfo<Emp> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP getDeptData(HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        // DEPT_HEAD 只能看到本部门，但部门列表可看全部（具体增删改在拦截器限制）
        return RESP.ok(empDao.getDeptData());
    }

    @Override
    public RESP getDutyData(HttpSession session) {
        return RESP.ok(empDao.getDutyData());
    }

    @Override
    public String add(Emp emp, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return "no_permission";

        // 权限校验：不能新增越权的职务
        String checkResult = AdminAuthUtil.checkEmpAddPermission(admin, emp.getDuty_id());
        if (checkResult != null) {
            return "no_permission";
        }

        // DEPT_HEAD 只能在本部门新增员工
        if (admin.isDeptHead()) {
            emp.setDept_id(admin.getDeptId());
        }

        return empDao.addEmp(emp) > 0 ? "true" : "false";
    }

    @Override
    public RESP update(int number, Emp emp, int currentPage, int pageSize, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return RESP.error(401, "未登录");

        // 查询目标员工的当前信息
        Emp targetEmp = empDao.selectByEmpNumber(number);
        if (targetEmp == null) return RESP.error("员工不存在");

        // 权限校验（含新职务校验：防止把普通员工改为部长）
        String checkResult = AdminAuthUtil.checkEmpModifyPermission(admin,
                targetEmp.getDept_id(), targetEmp.getDuty_id(), emp.getDuty_id());
        if (checkResult != null) {
            return RESP.error(403, checkResult);
        }

        // 执行更新
        emp.setNumber(number);
        empDao.updateEmp(emp);
        return selectByPage(currentPage, pageSize, session);
    }

    @Override
    public String delete(int number, HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return "no_permission";

        // 查询目标员工
        Emp targetEmp = empDao.selectByEmpNumber(number);
        if (targetEmp == null) return "false";

        // 权限校验
        String checkResult = AdminAuthUtil.checkEmpModifyPermission(admin,
                targetEmp.getDept_id(), targetEmp.getDuty_id());
        if (checkResult != null) {
            return "no_permission";
        }

        // 执行删除
        Emp emp = new Emp();
        emp.setNumber(number);
        empDao.deleteEmpSignByNumber(emp);
        empDao.deleteEmp(emp);
        return "true";
    }
}
