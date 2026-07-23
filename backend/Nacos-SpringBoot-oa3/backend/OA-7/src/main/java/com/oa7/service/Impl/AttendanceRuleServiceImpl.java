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
    public AttendanceRule getDefaultRule() {
        return attendanceRuleDao.selectDefault();
    }

    @Override
    public RESP getByDept(int deptId) {
        AttendanceRule rule = attendanceRuleDao.selectByDept(deptId);
        if (rule == null) {
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
        if (rule.getRule_name() == null || rule.getRule_name().trim().isEmpty()) {
            return RESP.error("规则名称不能为空");
        }
        if (rule.getWork_start_time() == null) {
            rule.setWork_start_time(LocalTime.of(9, 0));
        }
        if (rule.getWork_end_time() == null) {
            rule.setWork_end_time(LocalTime.of(18, 0));
        }
        if (rule.getLate_threshold_min() == null) {
            rule.setLate_threshold_min(0);
        }
        if (rule.getEarly_threshold_min() == null) {
            rule.setEarly_threshold_min(0);
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(true);
        }

        if (rule.getId() != null && rule.getId() > 0) {
            AttendanceRule existing = attendanceRuleDao.selectById(rule.getId());
            if (existing == null) {
                return RESP.error("规则不存在，无法更新");
            }
            if (rule.getDept_id() == null) {
                rule.setDept_id(existing.getDept_id());
            }
            attendanceRuleDao.update(rule);
            return RESP.ok("更新成功");
        } else {
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
        if (existing.getDept_id() == null) {
            return RESP.error("全局默认规则不可删除");
        }
        existing.setEnabled(false);
        attendanceRuleDao.update(existing);
        return RESP.ok("删除成功");
    }
}
