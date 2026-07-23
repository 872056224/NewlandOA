package com.oa7.controller;

import com.oa7.constant.AdminRole;
import com.oa7.dao.AdmDao;
import com.oa7.dao.EmpDao;
import com.oa7.pojo.Emp;
import com.oa7.util.AdminAuthUtil;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RBAC 辅助接口
 * 供员工服务(OA-2)调用，用于确定通知目标管理员
 */
@RestController
@RequestMapping("/rbac")
@CrossOrigin
public class RbacController {

    @Autowired
    private AdmDao admDao;

    @Autowired
    private EmpDao empDao;

    /**
     * 获取应接收通知的管理员列表
     * 员工提交请假/补签后，OA-2 调用此接口确定通知发给谁
     *
     * @param empNumber 提交申请的员工的 emp.number
     */
    @GetMapping("/notification-targets")
    public RESP getNotificationTargets(@RequestParam int empNumber) {
        // 1. 查询申请人信息
        Emp applicant = empDao.selectByEmpNumber(empNumber);
        if (applicant == null) {
            return RESP.error("员工不存在");
        }

        // 2. 获取所有有资格的管理员（已绑定、且职务为部长/副部长/董事长）
        List<Map<String, Object>> eligibleAdmins = admDao.selectAllEligibleAdmins();
        if (eligibleAdmins == null || eligibleAdmins.isEmpty()) {
            return RESP.ok(new ArrayList<>());
        }

        // 3. 筛选：通知谁？
        //    - CHAIRMAN（duty_id=17）→ 所有申请都通知
        //    - HR_DIRECTOR（dept_id=1, duty_id=1）→ 所有申请都通知
        //    - DEPT_HEAD（duty_id=1或2）→ 仅当 dept_id 匹配申请人的部门
        List<Map<String, Object>> targets = new ArrayList<>();
        for (Map<String, Object> admin : eligibleAdmins) {
            int dutyId = ((Number) admin.get("dutyId")).intValue();
            int deptId = ((Number) admin.get("deptId")).intValue();

            if (dutyId == 17) {
                // 董事长 → 通知
                targets.add(admin);
            } else if (deptId == 1 && dutyId == 1) {
                // 人事部部长 → 通知
                targets.add(admin);
            } else if ((dutyId == 1 || dutyId == 2) && deptId == applicant.getDept_id()) {
                // 本部门部长/副部长 → 通知
                targets.add(admin);
            }
        }

        return RESP.ok(targets);
    }
}
