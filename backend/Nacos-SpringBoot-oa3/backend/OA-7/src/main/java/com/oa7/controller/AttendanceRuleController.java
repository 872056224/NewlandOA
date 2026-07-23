package com.oa7.controller;

import com.oa7.pojo.AttendanceRule;
import com.oa7.service.AttendanceRuleService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/attendance-rules")
@CrossOrigin
public class AttendanceRuleController {

    @Autowired
    private AttendanceRuleService attendanceRuleService;

    /**
     * 获取全局默认规则
     */
    @GetMapping("/default")
    public RESP getDefault() {
        return attendanceRuleService.getDefault();
    }

    /**
     * 获取指定部门的规则
     */
    @GetMapping("/dept/{deptId}")
    public RESP getByDept(@PathVariable int deptId) {
        return attendanceRuleService.getByDept(deptId);
    }

    /**
     * 获取所有规则
     */
    @GetMapping
    public RESP getAll() {
        return attendanceRuleService.getAll();
    }

    /**
     * 新增规则
     */
    @PostMapping
    public RESP create(@RequestBody AttendanceRule rule) {
        rule.setId(null);
        return attendanceRuleService.save(rule);
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    public RESP update(@PathVariable int id, @RequestBody AttendanceRule rule) {
        rule.setId(id);
        return attendanceRuleService.save(rule);
    }

    /**
     * 删除规则（软删除）
     */
    @DeleteMapping("/{id}")
    public RESP delete(@PathVariable int id) {
        return attendanceRuleService.delete(id);
    }
}
