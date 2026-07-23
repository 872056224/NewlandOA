package com.oa2.service.impl;

import com.oa2.dao.AdminDao;
import com.oa2.dao.RetroactiveSignDao;
import com.oa2.pojo.RetroactiveSign;
import com.oa2.service.NotificationService;
import com.oa2.service.RetroactiveSignService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetroactiveSignServiceImpl implements RetroactiveSignService {

    @Autowired
    private RetroactiveSignDao retroactiveSignDao;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AdminDao adminDao;

    @Override
    public RESP apply(int number, String signDate, String type, String reason) {
        RetroactiveSign sign = new RetroactiveSign();
        sign.setNumber(number);
        sign.setSign_date(signDate);
        sign.setType(type);
        sign.setReason(reason);
        sign.setStatus("待审批");

        int ret = retroactiveSignDao.insert(sign);
        if (ret > 0) {
            String typeLabel = type.equals("a") ? "上午" : "下午";
            // 按角色通知相关管理员
            notifyAdmins(number, "retroactive_submitted", "新补签申请",
                    "员工提交了 " + signDate + "(" + typeLabel + ") 的补签申请",
                    String.valueOf(sign.getId()));
            return RESP.ok("提交成功");
        }
        return RESP.error("提交失败");
    }

    @Override
    public RESP getMyList(int number, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<RetroactiveSign> list = retroactiveSignDao.selectByNumberPage(number, offset, pageSize);
        int total = retroactiveSignDao.countByNumber(number);
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
