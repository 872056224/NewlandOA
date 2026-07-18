package com.oa7.controller;

import com.oa7.pojo.Admin;
import com.oa7.service.AdmService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AdmController {

    @Autowired
    private AdmService admService;

    @PostMapping("/login")
    public String login(@RequestBody Admin admin, HttpSession session) {
        return admService.login(admin, session);
    }

    @PostMapping("/register")
    public String register(@RequestBody Admin admin) {
        return admService.register(admin);
    }

    @GetMapping("/profile")
    public RESP getProfile(HttpSession session) {
        return admService.getProfile(session);
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        return admService.logout(session);
    }
}
