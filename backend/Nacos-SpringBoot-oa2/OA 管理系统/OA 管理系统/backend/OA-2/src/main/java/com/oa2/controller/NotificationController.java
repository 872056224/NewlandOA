package com.oa2.controller;

import com.oa2.pojo.Emp;
import com.oa2.service.NotificationService;
import com.oa2.util.RESP;
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
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return notificationService.getMyList(emp.getNumber(), currentPage, pageSize);
    }

    @GetMapping("/unread-count")
    public RESP unreadCount(HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return notificationService.getUnreadCount(emp.getNumber());
    }

    @PutMapping("/{id}/read")
    public RESP markRead(@PathVariable int id, HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return notificationService.markAsRead(emp.getNumber(), id);
    }

    @PutMapping("/read-all")
    public RESP markAllRead(HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        if (emp == null) return RESP.error("未登录");
        return notificationService.markAllAsRead(emp.getNumber());
    }
}
