package com.ccc.zerocodegenerateproject.ragModule.offLineManage;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.AiProjectSuggestion;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OffLineManagerImpl implements OffLineManager {

    private final dev.langchain4j.data.document.DocumentSplitter documentSplitter = DocumentSplitters.recursive(400,40);
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    // 切块
    @Override
    public List<TextSegment> getAllChunks(AiProjectSuggestion aiProjectSuggestion) {
        // 构建 Document
        Metadata metadata =Metadata.from(new HashMap<String,Object>());
        metadata.put("projectId", aiProjectSuggestion.getProjectId());
        metadata.put("projectName",aiProjectSuggestion.getProjectName());
        metadata.put("projectType",aiProjectSuggestion.getProjectType());
        metadata.put("techStack",aiProjectSuggestion.getTechStack());
        metadata.put("status", "SUCCESS");
        Document document = Document.from(String.valueOf(aiProjectSuggestion), metadata);

        // 切分得到切块
        return documentSplitter.split(document);
    }

    @Override
    public List<TextSegment> getAllChunks(AiProjectSuggestion aiProjectSuggestion, Metadata metadata){
        if (aiProjectSuggestion == null || StringUtils.isBlank((CharSequence) aiProjectSuggestion)) {
            return Collections.emptyList();
        }
        try {
            Document document = Document.from(String.valueOf(aiProjectSuggestion), metadata);
            return documentSplitter.split(document);
        } catch (Exception e) {
            log.error("生成 TextSegment 失败，projectId: {}",
                    aiProjectSuggestion.getProjectId(), e);
            return Collections.emptyList();
        }
    }

    // 向量化 + 存入redis
    @Override
    public void listEmbeddingsSave(List<TextSegment> allChunks){
        // 设置分批向量化参数
        int batchSize = 20;
        int totalSize = allChunks.size();
        List<Embedding> batchEmbeddings = new ArrayList<>();
        // 执行分批向量化 + 存入redis
        for (int i = 0; i < totalSize; i += batchSize) {
            int end = Math.min(i + batchSize, totalSize);
            List<TextSegment> batchChunks = allChunks.subList(i, end);  // 得到当前批次的分块数据集合
            batchEmbeddings = embeddingModel.embedAll(batchChunks).content();
            embeddingStore.addAll(batchEmbeddings, batchChunks);
        }
    }
}
