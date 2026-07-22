package com.oa.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI 对话服务：封装 ChatClient 的同步/流式调用、会话记忆与向量知识库管理
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    //Spring AI 核心客户端，负责与大模型（Ollama）通信、执行提示词、调用Advisor链
    private final ChatClient chatClient;

    //会话记忆管理器，存储和检索对话历史（最近20条），实现多轮对话上下文
    private final ChatMemory chatMemory;

    //向量数据库抽象接口（实际是Redis Stack），存储公司知识的向量表示，支持相似度检索
    private final VectorStore vectorStore;

    //Jackson JSON工具，用于流式输出时将token包装成 {"c":"内容"} 格式，避免SSE帧格式错误
    private final ObjectMapper objectMapper = new ObjectMapper();

    //Ollama 服务地址（本地大模型引擎的API端点）
    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    //对话模型名称
    @Value("${spring.ai.ollama.chat.options.model:qwen2.5:1.5b}")
    private String chatModel;
    //向量化模型名称（nomic-embed-text，负责将文本转成向量）
    @Value("${spring.ai.ollama.embedding.options.model:nomic-embed-text}")
    private String embeddingModel;
    //Redis Stack 即 (KnowledgeLoader 启动时将 company.md 切块并向量化后的结果），用于 RAG 检索时进行相似度搜索。
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    //Redis 端口（6380，避免与OA-7的6379冲突）
    @Value("${spring.data.redis.port:6380}")
    private int redisPort;

    /** 向量库模式：redis（默认）/ memory（内存降级，无需 Redis Stack） */
    @Value("${ai.assistant.vector-mode:redis}")
    private String vectorMode;

    public ChatService(ChatClient chatClient, ChatMemory chatMemory, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.vectorStore = vectorStore;
    }

    /**
     * 同步对话：阻塞等待完整回答（RAG 检索由 QuestionAnswerAdvisor 自动完成）
     */
    public String chat(String sessionId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
    }

    /** 开场推荐问题列表（与 knowledge/company.md 对应） */
    private static final List<String> SUGGESTION_QUESTIONS = List.of(
            "怎么签到打卡",
            "签到时间是怎么规定的",
            "忘记打卡漏签了怎么办",
            "怎么请假",
            "公司有哪些员工福利",
            "节假日有什么福利",
            "加班怎么算",
            "怎么修改密码",
            "忘记密码怎么办",
            "公司主营业务有哪些");

    /**
     * 同步对话 + 相关推荐：在聊天回复基础上，通过关键词匹配推荐相关问题
     */
    public Map<String, Object> chatWithRelated(String sessionId, String message) {
        String reply = chat(sessionId, message);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply", reply);
        result.put("sessionId", sessionId);

        // 关键词匹配推荐问题
        List<Map<String, Object>> relatedItems = new ArrayList<>();
        List<String> suggestionTexts = new ArrayList<>();
        String[] keywords = message.split("[，,、\\s]+");
        for (String q : SUGGESTION_QUESTIONS) {
            if (q.equals(message.trim())) continue;
            boolean match = false;
            for (String kw : keywords) {
                if (kw.length() >= 2 && q.contains(kw)) {
                    match = true;
                    break;
                }
            }
            if (match && relatedItems.size() < 3) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("question", q);
                item.put("score", 0.5);
                relatedItems.add(item);
                suggestionTexts.add(q);
            }
        }
        // 如果没匹配到，补前3个热门推荐
        if (relatedItems.isEmpty()) {
            for (int i = 0; i < Math.min(3, SUGGESTION_QUESTIONS.size()); i++) {
                String q = SUGGESTION_QUESTIONS.get(i);
                if (!q.equals(message.trim())) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("question", q);
                    item.put("score", 0.5);
                    relatedItems.add(item);
                    suggestionTexts.add(q);
                }
            }
        }
        result.put("related", relatedItems);
        result.put("suggestions", suggestionTexts);
        return result;
    }

    /**
     * 流式对话：SSE 输出。每个数据块包装为 JSON（{"c":"token"}），
     * 避免 token 内换行破坏 SSE 帧格式；结束块为 {"done":true}，错误块为 {"error":"..."}。
     */
    public Flux<String> chatStream(String sessionId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content()
                .map(token -> toJson("c", token))
                .concatWith(Flux.just("{\"done\":true}"))
                .onErrorResume(e -> {
                    log.error("AI 流式对话异常, sessionId={}", sessionId, e);
                    return Flux.just(toJson("error", friendlyError(e)), "{\"done\":true}");
                });
    }

    /**
     * 清空指定会话的对话记忆
     */
    public void clearMemory(String sessionId) {
        chatMemory.clear(sessionId);
        log.info("已清空会话记忆: {}", sessionId);
    }

    /**
     * 运行期动态添加知识到 Redis 向量库（实时生效，custom-* 前缀持久保留）
     *
     * @return 新知识块的文档 id
     */
    public String addKnowledge(String title, String content) {
        String docId = "custom-" + UUID.randomUUID();
        String text = (title == null || title.trim().isEmpty())
                ? content
                : "## " + title.trim() + "\n" + content;
        Document doc = Document.builder()
                .id(docId)
                .text(text)
                .metadata(Map.of(
                        "section", title == null ? "自定义知识" : title.trim(),
                        "source", "runtime",
                        "type", "custom"))
                .build();
        vectorStore.add(List.of(doc));
        log.info("已添加知识块至向量库: id={}, title={}", docId, title);
        return docId;
    }

    /**
     * 向量检索预览（调试/可观测）：查看某问题在向量库中的召回结果与相似度
     */
    public List<Map<String, Object>> searchKnowledge(String query, int topK) {
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build());
        List<Map<String, Object>> results = new ArrayList<>();
        if (documents == null) {
            return results;
        }
        for (Document doc : documents) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", doc.getId());
            item.put("score", doc.getScore());
            item.put("section", doc.getMetadata().get("section"));
            item.put("type", doc.getMetadata().get("type"));
            item.put("text", doc.getText());
            results.add(item);
        }
        return results;
    }

    /**
     * 健康探测：Ollama 可达性、双模型就绪度、Redis Stack 可达性
     */
    public Map<String, Object> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("model", chatModel);
        status.put("embeddingModel", embeddingModel);

        // Ollama 与模型就绪检查
        boolean chatReady = false;
        boolean embeddingReady = false;
        try {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
            factory.setReadTimeout((int) Duration.ofSeconds(3).toMillis());
            RestClient client = RestClient.builder()
                    .baseUrl(ollamaBaseUrl)
                    .requestFactory(factory)
                    .build();
            Map<?, ?> tags = client.get().uri("/api/tags").retrieve().body(Map.class);
            List<String> installed = new ArrayList<>();
            if (tags != null && tags.get("models") instanceof List<?> models) {
                for (Object m : models) {
                    if (m instanceof Map<?, ?> mm && mm.get("name") != null) {
                        installed.add(String.valueOf(mm.get("name")));
                    }
                }
            }
            chatReady = installed.stream().anyMatch(n -> n.startsWith(chatModel));
            embeddingReady = installed.stream().anyMatch(n -> n.startsWith(embeddingModel));
            status.put("ollama", true);
            status.put("installedModels", installed);
        } catch (Exception e) {
            status.put("ollama", false);
            status.put("hint", "Ollama 未启动或不可达（" + ollamaBaseUrl + "），请安装并启动 Ollama，"
                    + "再执行 ollama pull " + chatModel + " 与 ollama pull " + embeddingModel);
        }
        status.put("modelReady", chatReady);
        status.put("embeddingReady", embeddingReady);

        // 向量库就绪检查：memory 模式天然就绪；redis 模式检查 Redis Stack 可达性
        boolean memoryMode = "memory".equalsIgnoreCase(vectorMode);
        boolean redisUp = false;
        if (!memoryMode) {
            try (JedisPooled jedis = new JedisPooled(redisHost, redisPort)) {
                redisUp = "PONG".equalsIgnoreCase(jedis.ping());
            } catch (Exception e) {
                status.put("redisHint", "Redis Stack 不可达（" + redisHost + ":" + redisPort + "），"
                        + "可执行 docker run -d --name redis-stack -p 6380:6379 redis/redis-stack-server");
            }
        }
        status.put("vectorMode", memoryMode ? "memory" : "redis");
        status.put("redis", redisUp);
        // RAG 完整可用 = 向量化模型就绪 + 向量库就绪（内存模式或 Redis 可达）
        status.put("ragReady", embeddingReady && (memoryMode || redisUp));
        return status;
    }

    private String toJson(String key, String value) {
        try {
            return objectMapper.writeValueAsString(Map.of(key, value));
        } catch (Exception e) {
            return "{\"" + key + "\":\"\"}";
        }
    }

    private static String friendlyError(Throwable e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("Connection refused") || msg.contains("connect")) {
            return "无法连接本地大模型服务（Ollama），请确认已启动 Ollama 并拉取模型。";
        }
        return "AI 服务暂时不可用，请稍后再试。";
    }
}
