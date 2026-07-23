package com.oa2.service.impl;

import com.oa2.dao.AdminDao;
import com.oa2.dao.MakeupRequestDao;
import com.oa2.pojo.MakeupRequest;
import com.oa2.service.MakeupRequestService;
import com.oa2.service.NotificationService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MakeupRequestServiceImpl implements MakeupRequestService {

    @Autowired
    private MakeupRequestDao makeupRequestDao;

    @Autowired
    private AdminDao adminDao;

    @Autowired
    private NotificationService notificationService;

    @Override
    public RESP apply(int empId, String date, String type, String requestTime, String reason) {
        MakeupRequest req = new MakeupRequest();
        req.setEmpId(empId);
        req.setDate(date);
        req.setType(type);
        req.setRequestTime(requestTime);
        req.setReason(reason);

        int ret = makeupRequestDao.insert(req);
        if (ret > 0) {
            String typeLabel = "CHECK_IN".equals(type) ? "上班签到" : "下班签退";
            notifyAdmins(empId, "makeup_submitted", "新补卡申请",
                    "员工提交了 " + date + "(" + typeLabel + ") 的补卡申请",
                    String.valueOf(req.getId()));
            return RESP.ok("补卡申请已提交，等待管理员审批");
        }
        return RESP.error("提交失败");
    }

    @Override
    public RESP getMyList(int empId, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<MakeupRequest> list = makeupRequestDao.selectByEmpPage(empId, offset, pageSize);
        int total = makeupRequestDao.countByEmp(empId);
        return RESP.ok(list, currentPage, total);
    }

    /** 按角色通知相关管理员（董事长 + 人事部部长 + 本部门部长/副部长） */
    private void notifyAdmins(int applicantNumber, String type, String title, String content, String bizId) {
        try {
            List<Integer> adminIds = adminDao.selectNotifyTargetIds(applicantNumber);
            for (int adminId : adminIds) {
                notificationService.sendNotification(adminId, type, title, content, bizId);
            }
        } catch (Exception e) {
            System.err.println("通知管理员失败: " + e.getMessage());
        }
    }
}
