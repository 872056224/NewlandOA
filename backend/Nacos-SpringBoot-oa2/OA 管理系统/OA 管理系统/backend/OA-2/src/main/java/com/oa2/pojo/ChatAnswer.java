package com.oa2.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 客服问答响应体
 */
@Data
public class ChatAnswer {

    /** 回答内容 */
    private String answer;

    /** 是否命中知识库（false 表示兜底回复） */
    private boolean matched;

    /** 最佳匹配的相似度得分 [0,1]，保留两位小数 */
    private double confidence;

    /** 相关问题（命中时给出，可点击继续追问） */
    private List<Related> related = new ArrayList<>();

    /** 推荐问题（未命中时给出，引导用户提问） */
    private List<String> suggestions = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Related {
        private String question;
        private double score;
    }
}
