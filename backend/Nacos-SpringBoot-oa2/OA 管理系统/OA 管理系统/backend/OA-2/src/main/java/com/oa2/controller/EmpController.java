package com.oa2.controller;

import com.oa2.service.EmpService;
import com.oa2.pojo.Emp;
import com.oa2.service.impl.EmpServiceImpl;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping
@CrossOrigin
public class EmpController {

   @Autowired
   private EmpService service;

    @PostMapping("/login")
    public String login(@RequestBody Emp emp,HttpSession session) {
        return service.emplogin(emp,session);
    }

    @GetMapping("/profile")
    public RESP getprofile(HttpSession session){
        Emp emp = (Emp)session.getAttribute("emp");
        return RESP.ok(emp);
    }

    //oldPassword  旧的密码  用来和数据库进行匹配
    //emp.getPassword  新密码
    @PutMapping("/password")
    public String updatePassword(@RequestBody Emp emp, @RequestParam("oldPassword") String oldPassword) {

        return service.updateEmpPassword(emp, oldPassword);

    }

    @PutMapping("/profile")
    public RESP updateProfile(@RequestBody Emp emp, HttpSession session) {
        Emp loginEmp = (Emp) session.getAttribute("emp");
        if (loginEmp == null) {
            return RESP.error("未登录");
        }
        emp.setNumber(loginEmp.getNumber());
        int ret = service.updateEmp(emp);
        if (ret > 0) {
            // 更新 session 中的信息
            Emp updated = service.selectByNumber(emp.getNumber());
            if (updated != null) {
                session.setAttribute("emp", updated);
            }
            return RESP.ok("更新成功");
        }
        return RESP.error("更新失败");
    }
}
