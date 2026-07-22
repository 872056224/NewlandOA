package com.oa7.service.Impl;

import com.oa7.dao.NotificationDao;
import com.oa7.pojo.Notification;
import com.oa7.service.NotificationService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationDao notificationDao;

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
}
