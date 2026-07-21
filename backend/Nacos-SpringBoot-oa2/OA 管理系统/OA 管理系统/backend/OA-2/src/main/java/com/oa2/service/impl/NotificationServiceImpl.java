package com.oa2.service.impl;

import com.oa2.dao.NotificationDao;
import com.oa2.pojo.Notification;
import com.oa2.service.NotificationService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationDao notificationDao;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public RESP getMyList(int number, int currentPage, int pageSize) {
        int offset = (currentPage - 1) * pageSize;
        List<Notification> list = notificationDao.selectByTarget(number, offset, pageSize);
        int total = notificationDao.countByTarget(number);
        return RESP.ok(list, currentPage, total);
    }

    @Override
    public RESP getUnreadCount(int number) {
        int count = notificationDao.countUnreadByTarget(number);
        return RESP.ok(count);
    }

    @Override
    public RESP markAsRead(int number, int id) {
        notificationDao.markAsRead(id, number);
        return RESP.ok("操作成功");
    }

    @Override
    public RESP markAllAsRead(int number) {
        notificationDao.markAllAsRead(number);
        return RESP.ok("操作成功");
    }

    @Override
    public void sendNotification(int targetNumber, String type, String title, String content, String bizId) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setTarget_number(targetNumber);
        notification.setBiz_id(bizId);
        notificationDao.insert(notification);

        // WebSocket 实时推送
        if (messagingTemplate != null) {
            try {
                // 点对点推送: /queue/notifications 对应用户
                String destination = "/queue/notifications/" + targetNumber;
                messagingTemplate.convertAndSend(destination, notification);
            } catch (Exception e) {
                // WebSocket 推送失败不影响主流程
                System.err.println("WebSocket push failed: " + e.getMessage());
            }
        }
    }
}
