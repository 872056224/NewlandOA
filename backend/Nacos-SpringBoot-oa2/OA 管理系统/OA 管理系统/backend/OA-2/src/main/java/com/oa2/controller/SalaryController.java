package com.oa2.controller;

import com.oa2.dao.SalaryDao;
import com.oa2.pojo.Emp;
import com.oa2.pojo.SalaryDetail;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/salary")
@CrossOrigin
public class SalaryController {

    @Autowired
    private SalaryDao salaryDao;

    @GetMapping("/my/{yearMonth}")
    public RESP mySalary(@PathVariable String yearMonth, HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        SalaryDetail detail = salaryDao.selectByEmpAndMonth(emp.getNumber(), yearMonth);
        if (detail == null) return RESP.error("暂未核算");
        return RESP.ok(detail);
    }
}
