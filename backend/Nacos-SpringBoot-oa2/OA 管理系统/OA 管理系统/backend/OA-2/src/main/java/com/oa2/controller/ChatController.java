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
    @Value("${deepseek.api-key:}")
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

        try {
            // 收集知识库内容作为参考上下文
            List<KbDoc> allKb = kbDocDao.selectAllEnabled();
            StringBuilder kbContext = new StringBuilder();
            for (KbDoc doc : allKb) {
                kbContext.append("Q: ").append(doc.getQuestion()).append("\nA: ").append(doc.getAnswer()).append("\n\n");
            }

            // 直接调 DeepSeek，带上知识库上下文
            String answer = callDeepSeek(question, kbContext.toString());

            // 从知识库中提取相关问题作为推荐
            List<String> related = new ArrayList<>();
            for (KbDoc doc : allKb) {
                if (!doc.getQuestion().equals(question) && related.size() < 3) {
                    // 检查问题或关键词是否与用户提问相关
                    String[] keywords = question.split("[，,、\\s]+");
                    boolean match = false;
                    for (String kw : keywords) {
                        if (kw.length() >= 2 && (doc.getQuestion().contains(kw) ||
                            (doc.getKeywords() != null && doc.getKeywords().contains(kw)))) {
                            match = true;
                            break;
                        }
                    }
                    if (match) {
                        related.add(doc.getQuestion());
                    }
                }
            }
            // 如果关键词没匹配到，补充热门前3个
            if (related.isEmpty()) {
                List<String> hot = kbDocDao.selectHotQuestions();
                for (int i = 0; i < Math.min(3, hot.size()); i++) {
                    if (!hot.get(i).equals(question)) {
                        related.add(hot.get(i));
                    }
                }
            }

            JSONObject data = new JSONObject();
            data.put("answer", answer);
            data.put("related", related);
            data.put("suggestions", related);
            return RESP.ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("AI 服务暂时不可用：" + e.getMessage());
        }
    }

    private String callDeepSeek(String question, String kbContext) throws Exception {
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

        // 构建系统提示词：角色定位 + 知识库参考 + 格式约束
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是「小星」，星辰科技OA办公系统的AI智能客服助手。请以热情、专业的口吻回答用户的问题。\n\n");

        prompt.append("## 角色定位\n");
        prompt.append("- 你的名字叫「小星」，是星辰科技OA系统的AI助手\n");
        prompt.append("- 回答要热情友好，像个真实的客服人员在跟你聊天\n");
        prompt.append("- 控制在150字以内，简洁明了\n\n");

        prompt.append("## 知识库参考（优先参考以下内容回答）\n");
        if (kbContext != null && !kbContext.isEmpty()) {
            prompt.append(kbContext);
        } else {
            prompt.append("（暂无知识库内容）\n");
        }
        prompt.append("\n## 回答要求\n");
        prompt.append("1. 优先参考知识库内容回答问题\n");
        prompt.append("2. 如果知识库没有相关信息，根据你的知识正常回答\n");
        prompt.append("3. 回答控制在150字以内\n");
        prompt.append("4. 使用热情友好的语气\n");
        prompt.append("5. 直接回答问题，不要问「还有什么可以帮助您的吗」之类的客套话\n");

        systemMsg.put("content", prompt.toString());
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
