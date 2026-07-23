package com.oa7.controller;

import com.oa7.constant.AdminRole;
import com.oa7.dao.EmpDao;
import com.oa7.pojo.Admin;
import com.oa7.pojo.Emp;
import com.oa7.service.EmpService;
import com.oa7.service.SalaryService;
import com.oa7.util.AdminAuthUtil;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 管理员端 - 员工管理控制器
 */
@RestController
@RequestMapping("/employees")
@CrossOrigin
public class EmpController {

    @Autowired
    private EmpService empService;

    @Autowired
    private EmpDao empDao;

    @Autowired
    private SalaryService salaryService;

    @GetMapping
    public RESP list(@RequestParam int currentPage, @RequestParam int pageSize,
                     HttpSession session) {
        return empService.selectByPage(currentPage, pageSize, session);
    }

    @GetMapping("/departments")
    public RESP departments(HttpSession session) {
        return empService.getDeptData(session);
    }

    @GetMapping("/duties")
    public RESP duties(HttpSession session) {
        return empService.getDutyData(session);
    }

    @PostMapping
    public String add(@RequestBody Emp emp, HttpSession session) {
        return empService.add(emp, session);
    }

    @PutMapping("/{number}")
    public RESP update(@PathVariable int number, @RequestBody Emp emp,
                       @RequestParam int currentPage, @RequestParam int pageSize,
                       HttpSession session) {
        return empService.update(number, emp, currentPage, pageSize, session);
    }

    @DeleteMapping("/{number}")
    public String delete(@PathVariable int number, HttpSession session) {
        return empService.delete(number, session);
    }

    /** 更新员工自定义月薪（仅董事长和HR正副部长） */
    @PutMapping("/{number}/salary")
    public RESP updateSalary(@PathVariable int number,
                              @RequestBody Map<String, Object> body,
                              HttpSession session) {
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);
        if (admin == null) return RESP.error(401, "未登录");
        if (!admin.hasGlobalAccess() && admin.getRole() != AdminRole.DEPT_HEAD) {
            return RESP.error(403, "无权修改月薪");
        }
        // HR副部长可修改，但人事部部长也算HR_DIRECTOR已有权限
        Object salaryObj = body.get("baseSalary");
        BigDecimal salary = null;
        if (salaryObj != null) {
            salary = BigDecimal.valueOf(Double.parseDouble(salaryObj.toString()));
        }
        empDao.updateBaseSalary(number, salary);
        // 自动重新核算当月工资
        try {
            String ym = java.time.YearMonth.now().toString();
            salaryService.calculate(ym);
        } catch (Exception e) {
            System.err.println("自动重算工资失败: " + e.getMessage());
        }
        return RESP.ok("修改成功");
    }
}
