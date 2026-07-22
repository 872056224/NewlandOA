package com.oa7.service;

import com.oa7.util.RESP;

public interface NotificationService {
    RESP getMyList(int number, int currentPage, int pageSize);
    RESP getUnreadCount(int number);
    RESP markAsRead(int number, int id);
    RESP markAllAsRead(int number);
}
