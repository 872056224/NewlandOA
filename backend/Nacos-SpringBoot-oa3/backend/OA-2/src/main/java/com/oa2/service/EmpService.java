package com.oa2.service;

import com.oa2.pojo.Emp;
import com.oa2.util.RESP;

import javax.servlet.http.HttpSession;

public interface EmpService {

    //员工登录
    public String emplogin(Emp emp , HttpSession session);

    //更新密码
    public String updateEmpPwd(Emp emp,String oldpwd);

    //修改用户
    public RESP updateInfo(Emp emp,HttpSession session);

}
