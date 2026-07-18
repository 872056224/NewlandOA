package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.pojo.Department;
import com.oa7.service.EmpService;
import com.oa7.dao.EmpDao;
import com.oa7.pojo.Emp;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    public RESP selectByPage(int currentPage, int pageSize) {
        PageHelper.startPage(currentPage, pageSize);
        List<Emp> list = empDao.selectByPageHelper();
        PageInfo<Emp> pageInfo = new PageInfo<>(list);
        return RESP.ok(list, pageInfo.getPageNum(), (int) pageInfo.getTotal());
    }

    @Override
    public RESP getDeptData() {
        return RESP.ok(empDao.getDeptData());
    }

    @Override
    public RESP getDutyData() {
        return RESP.ok(empDao.getDutyData());
    }

    @Override
    public String add(Emp emp) {
        return empDao.addEmp(emp) > 0 ? "true" : "false";
    }

    @Override
    public RESP update(int number, Emp emp, int currentPage, int pageSize) {
        emp.setNumber(number);
        empDao.updateEmp(emp);
        return selectByPage(currentPage, pageSize);
    }

    @Override
    public String delete(int number) {
        Emp emp = new Emp();
        emp.setNumber(number);
        empDao.deleteEmpSignByNumber(emp);
        empDao.deleteEmp(emp);
        return "true";
    }
}
