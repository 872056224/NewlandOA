package com.oa2.service;

import com.oa2.pojo.ChatAnswer;

import java.util.List;

/**
 * AI 客服问答服务
 */
public interface ChatService {

    /**
     * 基于向量知识库回答用户问题
     *
     * @param question 用户问题（已校验非空）
     * @return 回答结果（含相似度、相关问题、推荐问题）
     */
    ChatAnswer ask(String question);

    /**
     * 获取推荐问题列表（用于开场引导与兜底推荐）
     *
     * @param count 期望条数
     */
    List<String> suggestions(int count);

    /**
     * 运行期向知识库动态添加一条知识（写入自建向量库并持久化）
     *
     * @return 新知识条目的 id
     */
    String addKnowledge(String question, String answer, String category);
}
