package com.oa2.service.impl;

import com.oa2.service.EmpService;
import com.oa2.dao.EmpDao;
import com.oa2.pojo.Emp;
import com.oa2.util.RESP;
import com.liuvei.common.SysFun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;


@Service
public class EmpServiceImpl implements EmpService {
    @Autowired
    private EmpDao empDao;

    //员工登录
    @Override
    public String emplogin(Emp emp , HttpSession session) {
        Emp emp1 = empDao.selectByNumber(emp);
        if (emp1 != null) {
            if (emp1.getPwd().equals(SysFun.md5(emp.getPwd()))) {
                session.setAttribute("emp" , emp1);
                return "true";
            }
        }
        return "false";
    }

    //更新密码
    @Override
    public String updateEmpPwd(Emp emp , String oldpwd) {
        Emp emp1 = empDao.selectByNumber(emp);
        if (emp1.getPwd().equals(SysFun.md5(oldpwd))) {
            emp.setPwd(SysFun.md5(emp.getPwd()));
            int r = empDao.updateEmpPwd(emp);
            if (r > 0) {
                return "true";
            }
        }
        return "false";
    }

    @Override
    public RESP updateInfo(Emp emp, HttpSession session) {
        int i=empDao.updateEmp(emp);
        if(i>0){
            Emp emp1 = empDao.selectByNumber(emp);
            session.setAttribute("emp",emp1);
            return RESP.ok(emp1);
        }
        return null;
    }
}
