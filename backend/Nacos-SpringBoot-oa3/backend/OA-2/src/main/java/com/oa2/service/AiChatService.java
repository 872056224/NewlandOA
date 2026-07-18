package com.oa2.service;

import com.oa2.util.RESP;

public interface AiChatService {

    /**
     * AI 客服问答
     *
     * @param question 用户问题
     * @return RESP.data 结构：
     *         answer          回答文本
     *         mode            kb=知识库直答 / llm=大模型生成 / suggest=猜你想问 / fallback=兜底
     *         matchedQuestion 命中的标准问题（未命中为 null）
     *         score           最高相似度得分
     *         suggestions     相关问题推荐列表
     */
    RESP chat(String question);

    /** 热门问题列表（对话页首屏推荐） */
    RESP hotQuestions();

    /** 从 MySQL 全量重建内存向量索引（管理端变更后调用） */
    RESP reloadIndex();
}
