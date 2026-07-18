package com.oa7.service;

import com.oa7.pojo.Admin;
import com.oa7.util.RESP;

import javax.servlet.http.HttpSession;

public interface AdmService {

    String login(Admin admin, HttpSession session);

    String register(Admin admin);

    RESP getProfile(HttpSession session);

    String logout(HttpSession session);
}
