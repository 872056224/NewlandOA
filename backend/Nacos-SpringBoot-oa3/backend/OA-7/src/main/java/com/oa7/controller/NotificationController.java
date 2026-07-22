package com.oa7.controller;

import com.oa7.pojo.Admin;
import com.oa7.service.NotificationService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/notifications")
@CrossOrigin
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public RESP list(@RequestParam(defaultValue = "1") int currentPage,
                     @RequestParam(defaultValue = "10") int pageSize,
                     HttpSession session) {
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) return RESP.error("未登录");
        return notificationService.getMyList(admin.getId(), currentPage, pageSize);
    }

    @GetMapping("/unread-count")
    public RESP unreadCount(HttpSession session) {
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) return RESP.error("未登录");
        return notificationService.getUnreadCount(admin.getId());
    }

    @PutMapping("/{id}/read")
    public RESP markRead(@PathVariable int id, HttpSession session) {
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) return RESP.error("未登录");
        return notificationService.markAsRead(admin.getId(), id);
    }

    @PutMapping("/read-all")
    public RESP markAllRead(HttpSession session) {
        Admin admin = (Admin) session.getAttribute("admin");
        if (admin == null) return RESP.error("未登录");
        return notificationService.markAllAsRead(admin.getId());
    }
}
