package com.oa2.vector;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向量检索命中结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private VectorDocument document;

    /** 余弦相似度得分，范围 [-1, 1]，越大越相似 */
    private double score;
}
