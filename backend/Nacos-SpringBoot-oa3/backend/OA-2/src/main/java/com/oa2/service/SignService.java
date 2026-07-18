package com.oa2.service;

import com.oa2.pojo.Emp;
import com.oa2.pojo.Sign;
import com.oa2.util.RESP;

import javax.servlet.http.HttpSession;


public interface SignService {

    //查找员工今日签到任务
    public RESP empSignList(HttpSession session);

    //员工签到
    RESP updateState(Sign sign ,HttpSession session,String cor);

    //使用pagehelper实现用户签到分页查询
    public RESP selectByPagehelper(int currentPage,int pageSize,HttpSession session);

    //分页查询
    public RESP selectByPage(int currentPage,int pageSize,HttpSession session);







}
