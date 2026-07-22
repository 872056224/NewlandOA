package com.oa.ai.controller;

import com.oa.ai.service.ChatService;
import com.oa.ai.util.RESP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能客服对话接口
 *
 * 经网关访问的完整前缀：/api/v1/ai/chat
 */
@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /** 开场推荐问题（与 knowledge/company.md 章节对应） */
    private static final List<String> SUGGESTIONS = List.of(
            "怎么签到打卡",
            "签到时间是怎么规定的",
            "忘记打卡漏签了怎么办",
            "怎么请假",
            "公司有哪些员工福利",
            "节假日有什么福利",
            "加班怎么算",
            "怎么修改密码",
            "忘记密码怎么办",
            "公司主营业务有哪些"
    );

    /** 单次提问最大长度（防御超长输入打爆模型上下文） */
    private static final int MAX_MESSAGE_LENGTH = 500;

    /** 会话 ID 最大长度（防御异常输入） */
    private static final int MAX_SESSION_ID_LENGTH = 64;

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 同步对话
     * Body: {"sessionId": "emp-121", "message": "公司有哪些员工福利？"}
     */
    @PostMapping
    public RESP chat(@RequestBody(required = false) Map<String, String> body) {
        System.out.println("执行此处的同步对话");
        String error = validate(body);
        if (error != null) {
            return RESP.error(400, error);
        }
        String sessionId = normalizeSessionId(body.get("sessionId"));
        String message = body.get("message").trim();
        try {
            //调用带相关推荐的增强对话（回复 + 相关问题词条）
            Map<String, Object> data = chatService.chatWithRelated(sessionId, message);
            return RESP.ok(data);
        } catch (Exception e) {
            log.error("AI 同步对话异常, sessionId={}", sessionId, e);
            return RESP.error("AI 服务暂时不可用，请确认本地 Ollama 已启动并已拉取模型");
        }
    }

    /**
     * 流式对话（SSE）：数据块为 JSON 字符串 {"c":"token"}，结束块 {"done":true}
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody(required = false) Map<String, String> body) {
        System.out.println("执行此处的流式对话");
        String error = validate(body);
        if (error != null) {
            return Flux.just("{\"error\":\"" + error + "\"}", "{\"done\":true}");
        }
        String sessionId = normalizeSessionId(body.get("sessionId"));
        String message = body.get("message").trim();
        return chatService.chatStream(sessionId, message);
    }

    /**
     * 清空会话记忆（开启新话题）
     */
    @DeleteMapping("/{sessionId}")
    public RESP clearMemory(@PathVariable String sessionId) {
        System.out.println("清空对话记录......");
        try {
            chatService.clearMemory(normalizeSessionId(sessionId));
            return RESP.ok("会话记忆已清空");
        } catch (Exception e) {
            log.error("清空会话记忆异常, sessionId={}", sessionId, e);
            return RESP.error("清空会话记忆失败");
        }
    }

    /**
     * 健康探测：前端据此显示「大模型在线 / 离线降级」状态
     */
    @GetMapping("/health")
    public RESP health() {
        return RESP.ok(chatService.health());
    }

    /**
     * 开场推荐问题（前端「新对话」时展示的词条）
     */
    @GetMapping("/suggestions")
    public RESP suggestions() {
        return RESP.ok(SUGGESTIONS);
    }

    /**
     * 动态添加知识到 Redis 向量库（实时生效）
     * Body: {"title": "差旅报销制度", "content": "出差住宿标准为..."}
     */
    @PostMapping("/kb")
    public RESP addKnowledge(@RequestBody(required = false) Map<String, String> body) {
        String content = body == null ? null : body.get("content");
        if (content == null || content.trim().isEmpty()) {
            return RESP.error(400, "知识内容 content 不能为空");
        }
        if (content.length() > 2000) {
            return RESP.error(400, "单条知识过长，请控制在 2000 字以内（建议按主题拆分多条）");
        }
        try {
            String id = chatService.addKnowledge(body.get("title"), content.trim());
            return RESP.ok(id);
        } catch (Exception e) {
            log.error("添加知识块异常", e);
            return RESP.error("添加知识失败，请确认 Redis Stack 与向量化模型已就绪");
        }
    }

    /**
     * 向量检索预览（调试/可观测）：查看 RAG 对某问题的召回结果与相似度
     */
    @GetMapping("/kb/search")
    public RESP searchKnowledge(@RequestParam("q") String query,
                                @RequestParam(value = "topK", defaultValue = "4") int topK) {
        if (query == null || query.trim().isEmpty()) {
            return RESP.error(400, "查询参数 q 不能为空");
        }
        try {
            return RESP.ok(chatService.searchKnowledge(query.trim(), Math.min(Math.max(topK, 1), 10)));
        } catch (Exception e) {
            log.error("向量检索预览异常, q={}", query, e);
            return RESP.error("检索失败，请确认 Redis Stack 与向量化模型已就绪");
        }
    }

    /** 入参校验，返回 null 表示通过 */
    private static String validate(Map<String, String> body) {
        String message = body == null ? null : body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return "消息内容不能为空";
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            return "消息过长，请控制在 " + MAX_MESSAGE_LENGTH + " 字以内";
        }
        return null;
    }

    /** 会话 ID 规范化：缺省给 default，超长截断 */
    private static String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return "default";
        }
        String sid = sessionId.trim();
        return sid.length() > MAX_SESSION_ID_LENGTH ? sid.substring(0, MAX_SESSION_ID_LENGTH) : sid;
    }
}
