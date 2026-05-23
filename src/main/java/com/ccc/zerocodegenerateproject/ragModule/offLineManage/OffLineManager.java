package com.ccc.zerocodegenerateproject.ragModule.offLineManage;

import com.ccc.zerocodegenerateproject.projectManager.domain.dto.AiProjectSuggestion;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

public interface OffLineManager {

    // 切块
    List<TextSegment> getAllChunks(AiProjectSuggestion aiProjectSuggestion);

    // 按照元数据类型过滤被切块内容
    List<TextSegment> getAllChunks(AiProjectSuggestion aiProjectSuggestion, Metadata metadata);

    // 向量化 + 存入 redis
    void listEmbeddingsSave(List<TextSegment> allChunks);
}
