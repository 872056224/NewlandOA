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

    @Override
    public String emplogin(Emp emp, HttpSession session) {

        //1 调用MyBatis(持久层)来访问数据库 判断用户与密码是否正确
        Emp emp1 = empDao.selectByNumber(emp);
        //202cb962ac59075b964b07152d234b70   -   123123
        if(emp1!=null){
            // 校验MD5的密码是否正确
            if(emp1.getPwd().equals(SysFun.md5(emp.getPwd()))){
                //存入到Session中
                session.setAttribute("emp",emp1);
                return  "true";
            }
        }
        return "false";
    }

    @Override
    public String updateEmpPassword(Emp emp, String oldpassword) {
        //调用数据库查询用户是否存在
        Emp emp1 = empDao.selectByNumber(emp);
        //查询旧的密码是否正确
        if(emp1.getPwd().equals(SysFun.md5(oldpassword))){
            //新的密码注入到对象中
            emp.setPwd(SysFun.md5(emp.getPwd()));

            int ret= empDao.updateEmpPwd(emp);
            if(ret>0){
                return "true";
            }
        }
        return "false";
    }

    @Override
    public int updateEmp(Emp emp) {
        return empDao.updateEmp(emp);
    }

    @Override
    public Emp selectByNumber(int number) {
        return empDao.selectByEmpNumber(number);
    }
}
