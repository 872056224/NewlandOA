package com.oa7.controller;

import com.oa7.service.DeptService;
import com.oa7.pojo.Department;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员端 - 部门管理控制器
 */
@RestController
@RequestMapping("/departments")
@CrossOrigin
public class DeptController {

    @Autowired
    private DeptService deptService;

    @GetMapping
    public RESP list(@RequestParam(defaultValue = "1") int currentPage,
                     @RequestParam(defaultValue = "10") int pageSize) {
        return deptService.selectByPage(currentPage, pageSize);
    }

    @PostMapping
    public RESP add(@RequestBody Department dept,
                    @RequestParam(defaultValue = "1") int currentPage,
                    @RequestParam(defaultValue = "10") int pageSize) {
        return deptService.add(dept, currentPage, pageSize);
    }

    @PutMapping("/{deptId}")
    public RESP update(@PathVariable int deptId, @RequestBody Department dept,
                       @RequestParam(defaultValue = "1") int currentPage,
                       @RequestParam(defaultValue = "10") int pageSize) {
        return deptService.update(deptId, dept, currentPage, pageSize);
    }
}
