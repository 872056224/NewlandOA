package com.oa7.service.Impl;

import com.oa7.dao.AttendanceRuleDao;
import com.oa7.pojo.AttendanceRule;
import com.oa7.service.AttendanceRuleService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
public class AttendanceRuleServiceImpl implements AttendanceRuleService {

    @Autowired
    private AttendanceRuleDao attendanceRuleDao;

    @Override
    public RESP getDefault() {
        AttendanceRule rule = attendanceRuleDao.selectDefault();
        if (rule == null) {
            return RESP.error("未配置默认考勤规则");
        }
        return RESP.ok(rule);
    }

    @Override
    public RESP getByDept(int deptId) {
        AttendanceRule rule = attendanceRuleDao.selectByDept(deptId);
        if (rule == null) {
            // 部门没有专用规则时，返回默认规则
            AttendanceRule defaultRule = attendanceRuleDao.selectDefault();
            return RESP.ok(defaultRule);
        }
        return RESP.ok(rule);
    }

    @Override
    public RESP getAll() {
        List<AttendanceRule> list = attendanceRuleDao.selectAll();
        return RESP.ok(list);
    }

    @Override
    public RESP save(AttendanceRule rule) {
        // 校验必填字段
        if (rule.getRuleName() == null || rule.getRuleName().trim().isEmpty()) {
            return RESP.error("规则名称不能为空");
        }
        if (rule.getWorkStartTime() == null) {
            rule.setWorkStartTime(LocalTime.of(9, 0));
        }
        if (rule.getWorkEndTime() == null) {
            rule.setWorkEndTime(LocalTime.of(18, 0));
        }
        if (rule.getLateThresholdMin() == null) {
            rule.setLateThresholdMin(0);
        }
        if (rule.getEarlyThresholdMin() == null) {
            rule.setEarlyThresholdMin(0);
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }

        if (rule.getId() != null && rule.getId() > 0) {
            // 更新
            AttendanceRule existing = attendanceRuleDao.selectById(rule.getId());
            if (existing == null) {
                return RESP.error("规则不存在，无法更新");
            }
            // 保留原有 deptId 如果没传
            if (rule.getDeptId() == null) {
                rule.setDeptId(existing.getDeptId());
            }
            attendanceRuleDao.update(rule);
            return RESP.ok("更新成功");
        } else {
            // 新增
            attendanceRuleDao.insert(rule);
            return RESP.ok(rule);
        }
    }

    @Override
    public RESP delete(int id) {
        AttendanceRule existing = attendanceRuleDao.selectById(id);
        if (existing == null) {
            return RESP.error("规则不存在");
        }
        // 不允许删除全局默认规则
        if (existing.getDeptId() == null) {
            return RESP.error("全局默认规则不可删除");
        }
        // 软删除：设置 enabled = false
        existing.setEnabled(false);
        attendanceRuleDao.update(existing);
        return RESP.ok("删除成功");
    }
}
