package com.oa7.service.Impl;

import com.oa7.dao.AdmDao;
import com.oa7.pojo.Admin;
import com.oa7.service.AdmService;
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
        Admin admin1 = admDao.selectByName(admin);
        if (admin1 != null) {
            if (admin1.getPwd().equals(SysFun.md5(admin.getPwd())) ||
                admin1.getPwd().equals(admin.getPwd())) {
                session.setAttribute("admin", admin1);
                return "true";
            }
        }
        return "false";
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
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) {
            return RESP.error(401, "未登录");
        }
        return RESP.ok(admin);
    }

    @Override
    public String logout(HttpSession session) {
        session.invalidate();
        return "true";
    }
}
