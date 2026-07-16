package com.oa2.service;

import com.oa2.pojo.Emp;
import com.oa2.util.RESP;

import javax.servlet.http.HttpSession;

public interface EmpService {

    /**
     * 登录
     * @param emp  用户对象
     * @param session
     * @return
     */
    public String emplogin(Emp emp,HttpSession session);

    /**
     * 更新密码
     * @param emp
     * @param oldpassword
     * @return
     */
    public String updateEmpPassword(Emp emp,String oldpassword);

    /**
     * 更新个人信息
     * @param emp
     * @return 影响行数
     */
    public int updateEmp(Emp emp);

    /**
     * 根据工号查询员工
     * @param number
     * @return
     */
    public Emp selectByNumber(int number);


}
