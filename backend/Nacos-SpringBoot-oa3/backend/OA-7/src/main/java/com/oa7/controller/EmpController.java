package com.oa7.controller;

import com.oa7.pojo.Emp;
import com.oa7.service.EmpService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端 - 员工管理控制器
 */
@RestController
@RequestMapping("/employees")
@CrossOrigin
public class EmpController {

    @Autowired
    private EmpService empService;

    @GetMapping
    public RESP list(@RequestParam int currentPage, @RequestParam int pageSize) {
        return empService.selectByPage(currentPage, pageSize);
    }

    @GetMapping("/departments")
    public RESP departments() {
        return empService.getDeptData();
    }

    @GetMapping("/duties")
    public RESP duties() {
        return empService.getDutyData();
    }

    @PostMapping
    public String add(@RequestBody Emp emp) {
        return empService.add(emp);
    }

    @PutMapping("/{number}")
    public RESP update(@PathVariable int number, @RequestBody Emp emp,
                       @RequestParam int currentPage, @RequestParam int pageSize) {
        return empService.update(number, emp, currentPage, pageSize);
    }

    @DeleteMapping("/{number}")
    public String delete(@PathVariable int number) {
        return empService.delete(number);
    }
}
