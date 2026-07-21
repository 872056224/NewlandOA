package com.oa2.service;

import com.oa2.util.RESP;

public interface NotificationService {
    RESP getMyList(int number, int currentPage, int pageSize);
    RESP getUnreadCount(int number);
    RESP markAsRead(int number, int id);
    RESP markAllAsRead(int number);
    void sendNotification(int targetNumber, String type, String title, String content, String bizId);
}
