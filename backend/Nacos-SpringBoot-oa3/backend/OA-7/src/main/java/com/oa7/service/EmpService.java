package com.oa7.service;

import com.oa7.pojo.Emp;
import com.oa7.util.RESP;

import javax.servlet.http.HttpSession;

/**
 * @name: chenle
 * @Date: 2021/12/3 14:44
 * @Author: IAO
 * @Description: ...
 */
public interface EmpService {

    RESP selectByPage(int currentPage, int pageSize, HttpSession session);

    RESP getDeptData(HttpSession session);

    RESP getDutyData(HttpSession session);

    String add(Emp emp, HttpSession session);

    RESP update(int number, Emp emp, int currentPage, int pageSize, HttpSession session);

    String delete(int number, HttpSession session);
}
