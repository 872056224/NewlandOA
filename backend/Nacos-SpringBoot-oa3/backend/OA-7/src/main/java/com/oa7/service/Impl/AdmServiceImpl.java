package com.oa7.service.Impl;

import com.oa7.constant.AdminRole;
import com.oa7.dao.AdmDao;
import com.oa7.pojo.Admin;
import com.oa7.service.AdmService;
import com.oa7.util.AdminAuthUtil;
import com.oa7.util.RESP;
import com.liuvei.common.SysFun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

@Service
public class AdmServiceImpl implements AdmService {

    @Autowired
    private AdmDao admDao;

    @Override
    public String login(Admin admin, HttpSession session) {
        // 1. 联表查询 admin + emp 信息
        Admin admin1 = admDao.selectAdminWithEmpByName(admin.getName());
        if (admin1 == null) {
            return "false"; // 账号不存在
        }

        // 2. 验证密码（支持 MD5 和明文）
        if (!admin1.getPwd().equals(SysFun.md5(admin.getPwd())) &&
            !admin1.getPwd().equals(admin.getPwd())) {
            return "false";
        }

        // 3. 检查是否绑定了员工
        if (admin1.getEmpNumber() == null || admin1.getEmpNumber() == 0) {
            return "no_emp_binding";
        }

        // 4. 检查员工信息是否完整
        if (admin1.getDeptId() == null || admin1.getDutyId() == null) {
            return "emp_not_found";
        }

        // 5. 计算角色
        AdminRole role = AdminAuthUtil.computeRole(admin1.getDeptId(), admin1.getDutyId());
        if (role == null) {
            return "no_permission"; // 普通员工不能登录管理端
        }
        admin1.setRole(role);

        // 6. 存入 Session
        session.setAttribute("admin", admin1);
        return "true";
    }

    @Override
    public String register(Admin admin) {
        if (admDao.selectByName(admin) != null) {
            return "false";
        }
        admin.setPwd(SysFun.md5(admin.getPwd()));
        return admDao.insertAdm(admin) > 0 ? "true" : "false";
    }

    @Override
    public RESP getProfile(HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) {
            return RESP.error(401, "未登录");
        }

        // 重新查询，获取最新信息
        Admin freshAdmin = admDao.selectAdminWithEmpById(admin.getId());
        if (freshAdmin != null && freshAdmin.getEmpNumber() != null) {
            // 重新计算角色
            if (freshAdmin.getDeptId() != null && freshAdmin.getDutyId() != null) {
                freshAdmin.setRole(AdminAuthUtil.computeRole(freshAdmin.getDeptId(), freshAdmin.getDutyId()));
            }
            session.setAttribute("admin", freshAdmin);
            return RESP.ok(freshAdmin);
        }

        // 降级返回基础信息
        return RESP.ok(admin);
    }

    @Override
    public String logout(HttpSession session) {
        session.invalidate();
        return "true";
    }
}
