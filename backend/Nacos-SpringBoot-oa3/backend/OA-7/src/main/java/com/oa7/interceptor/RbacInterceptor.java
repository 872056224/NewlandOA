package com.oa7.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa7.constant.AdminRole;
import com.oa7.pojo.Admin;
import com.oa7.util.AdminAuthUtil;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * RBAC 权限拦截器
 * 1. 验证登录状态
 * 2. 根据角色限制接口访问权限
 */
public class RbacInterceptor implements HandlerInterceptor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        Admin admin = AdminAuthUtil.getCurrentAdmin(session);

        // 1. 验证登录
        if (admin == null) {
            writeJsonResponse(response, 401, "未登录或登录已过期，请重新登录");
            return false;
        }

        // 2. 验证角色存在
        if (admin.getRole() == null) {
            writeJsonResponse(response, 403, "无管理端权限");
            return false;
        }

        // 3. 路径级别的角色校验
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 考勤规则管理 → 仅 CHAIRMAN 和 HR_DIRECTOR 可访问
        if (path.contains("/attendance-rules") || path.contains("/attendance/rules")) {
            if (!admin.hasGlobalAccess()) {
                writeJsonResponse(response, 403, "无权访问考勤规则");
                return false;
            }
        }

        // 节假日管理 → DEPT_HEAD 只读
        if (path.contains("/holidays")) {
            if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
                if (!admin.hasGlobalAccess()) {
                    writeJsonResponse(response, 403, "无权修改节假日");
                    return false;
                }
            }
        }

        // 部门管理（写操作）→ 仅 CHAIRMAN 和 HR_DIRECTOR
        if (path.contains("/departments")) {
            if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
                if (!admin.hasGlobalAccess()) {
                    writeJsonResponse(response, 403, "无权管理部门");
                    return false;
                }
            }
        }

        // 职务管理（写操作）→ 仅 CHAIRMAN 和 HR_DIRECTOR
        if (path.contains("/duties")) {
            if ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
                if (!admin.hasGlobalAccess()) {
                    writeJsonResponse(response, 403, "无权管理职务");
                    return false;
                }
            }
        }

        return true;
    }

    private void writeJsonResponse(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code); // 使用真实的 HTTP 状态码
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> result = new HashMap<>();
        result.put("code", code);
        result.put("message", message);
        result.put("data", null);

        PrintWriter writer = response.getWriter();
        writer.write(objectMapper.writeValueAsString(result));
        writer.flush();
        writer.close();
    }
}
