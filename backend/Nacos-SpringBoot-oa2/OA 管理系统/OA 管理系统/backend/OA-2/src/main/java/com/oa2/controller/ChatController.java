package com.oa2.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.oa2.dao.KbDocDao;
import com.oa2.pojo.KbDoc;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@CrossOrigin
public class ChatController {

    private static final String DEEPSEEK_API_URL = "https://api.deepseek.com/v1/chat/completions";
    @Value("${deepseek.api-key}")
    private String apiKey;
    private static final String DEEPSEEK_MODEL = "deepseek-chat";

    @Autowired
    private KbDocDao kbDocDao;

    @GetMapping("/suggestions")
    public RESP getSuggestions() {
        List<String> questions = kbDocDao.selectHotQuestions();
        return RESP.ok(questions);
    }

    @PostMapping("/ask")
    public RESP ask(@RequestBody Map<String, String> body) {
        String question = body.get("question");
        if (question == null || question.trim().isEmpty()) {
            return RESP.error("问题不能为空");
        }
        question = question.trim();

        // 1. 先搜知识库
        KbDoc kb = kbDocDao.searchByKeyword(question);
        if (kb != null) {
            JSONObject data = new JSONObject();
            data.put("answer", kb.getAnswer());
            List<KbDoc> all = kbDocDao.selectAllEnabled();
            List<String> related = new ArrayList<>();
            for (KbDoc doc : all) {
                if (!doc.getQuestion().equals(kb.getQuestion()) && related.size() < 3) {
                    related.add(doc.getQuestion());
                }
            }
            data.put("related", related);
            data.put("suggestions", related);
            return RESP.ok(data);
        }

        // 2. 调 DeepSeek
        try {
            String answer = callDeepSeek(question);
            JSONObject data = new JSONObject();
            data.put("answer", answer);
            data.put("related", new ArrayList<>());
            data.put("suggestions", new ArrayList<>());
            return RESP.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("AI 服务暂时不可用：" + e.getMessage());
        }
    }

    private String callDeepSeek(String question) throws Exception {
        URL url = new URL(DEEPSEEK_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        JSONObject body = new JSONObject();
        body.put("model", DEEPSEEK_MODEL);
        body.put("stream", false);

        JSONArray messages = new JSONArray();

        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "你是一个OA办公系统的AI客服助手，你的名字叫「小星」。请用中文简洁回答OA系统使用相关问题，包括签到打卡、请假、个人信息修改等。如果问题与OA系统无关，礼貌地引导回OA话题。回答控制在200字以内。");
        messages.add(systemMsg);

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", question);
        messages.add(userMsg);

        body.put("messages", messages);

        byte[] input = body.toJSONString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        BufferedReader reader;
        if (code == 200) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
        }
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (code != 200) {
            JSONObject err = JSONObject.parseObject(response.toString());
            String errMsg = err.getString("error") != null ? err.getString("error") : "HTTP " + code;
            throw new RuntimeException("DeepSeek API 错误: " + errMsg);
        }

        JSONObject result = JSONObject.parseObject(response.toString());
        JSONArray choices = result.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject choice = choices.getJSONObject(0);
            JSONObject message = choice.getJSONObject("message");
            if (message != null) {
                return message.getString("content");
            }
        }
        return "抱歉，我没有理解您的问题，请换个方式描述。";
    }
}
