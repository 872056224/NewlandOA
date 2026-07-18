package com.oa7.service;

import com.oa7.pojo.Emp;
import com.oa7.util.RESP;


/**
 * @name: chenle
 * @Date: 2021/12/3 14:44
 * @Author: IAO
 * @Description: ...
 */
public interface EmpService {

    RESP selectByPage(int currentPage, int pageSize);

    RESP getDeptData();

    RESP getDutyData();

    String add(Emp emp);

    RESP update(int number, Emp emp, int currentPage, int pageSize);

    String delete(int number);
}
