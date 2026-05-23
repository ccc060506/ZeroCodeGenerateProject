package com.ccc.zerocodegenerateproject.projectManager.service.impl;

import com.ccc.zerocodegenerateproject.projectManager.domain.ai.ProjectCreationAiService;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.AiProjectSuggestion;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.ProjectRankDTO;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import com.ccc.zerocodegenerateproject.projectManager.domain.vo.ProjectRankVO;
import com.ccc.zerocodegenerateproject.projectManager.mapper.ProjectMapper;
import com.ccc.zerocodegenerateproject.projectManager.service.AiService;
import com.ccc.zerocodegenerateproject.projectManager.service.ProjectService;
import com.ccc.zerocodegenerateproject.ragModule.config.RagConfig;
import com.ccc.zerocodegenerateproject.ragModule.offLineManage.OffLineManager;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.UserLlmConfig;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final ProjectMapper projectMapper;
    private final OffLineManager offLineManager;
    private final ProjectService projectService;
    private final RagConfig ragConfig;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    // 根据提示词推断 project 的相关属性
    @Override
    public AiProjectSuggestion parseProjectFromPrompt(String prompt, UserLlmConfig config) {
        java.net.http.HttpClient.Builder proxyBuilder = java.net.http.HttpClient.newBuilder()
                .proxy(java.net.ProxySelector.of(new java.net.InetSocketAddress("127.0.0.1", 7897)))
                .connectTimeout(java.time.Duration.ofSeconds(60));
        try {
            // 根据用户的 config 动态构建 StreamingChatModel
            OpenAiChatModel model = OpenAiChatModel.builder()
                    .httpClientBuilder(JdkHttpClient.builder().httpClientBuilder(proxyBuilder))
                    .apiKey(config.getApiKey())
                    .baseUrl(config.getBaseUrl())
                    .modelName(config.getModelName())
                    .temperature(config.getTemperature())
                    .timeout(Duration.ofSeconds(120))
                    .logRequests(true)
                    .logResponses(true)
                    .build();
            // 使用 LangChain4j 的 AiServices 动态构建接口实现
            ProjectCreationAiService dynamicService = AiServices.builder(ProjectCreationAiService.class)
                    .chatModel(model)
                    .contentRetriever(ragConfig.myRetriever(embeddingStore, embeddingModel))  // redis向量化数据库和模型
                    .systemMessageProvider(chatRequest -> buildSystemMessageWithExcellentCases(prompt)) // 把检索结果拼接到 System Prompt
                    .build();

            log.info("成功构建 AiService，model: {}", model.getClass().getSimpleName());

            // 调用并返回
            return dynamicService.createProjectSuggestion(prompt);
        } catch (Exception e) {
            log.error("动态调用 AI 失败，用户ID: {}", config.getUserId(), e);
            return fallbackSuggestion(prompt);
        }
    }

    /**
     * AI 调用失败时的降级方案
     */
    private AiProjectSuggestion fallbackSuggestion(String prompt) {
        return AiProjectSuggestion.builder()
                .projectName("AI生成项目-" + System.currentTimeMillis())
                .description(prompt)
                .projectType("管理后台")
                .techStack("react_ant")
                .build();
    }

    /**
     * 动态构建带优秀案例的 SystemMessage
     */
    private String buildSystemMessageWithExcellentCases(String userPrompt) {

        // 从向量库检索最相关的优秀案例
        String retrievedContents = retrieveExcellentCases(userPrompt);

        // 构建完整的 SystemMessage
        return """
        你是专业的零代码生成平台项目创建助手。
        
        这里有一些平台上优秀的项目案例和组件规范，供你严格参考它们的结构、命名风格和组件组织方式：
        
        %s
        
        用户会输入一句话需求，你必须严格按照以下规则返回结果：
        - 只返回一个有效的 JSON 对象，不要添加任何额外文字、解释、markdown 或 ```json 标记。
        - 返回的 JSON 必须完全符合 AiProjectSuggestion 类的结构。
        - projectType 只能是：CRM、OA、管理后台、H5应用、内部工具、电商、其他
        - techStack 只能是：react_ant、vue_element、uniapp、html5、其他
        - 请生成一个简洁、专业、有吸引力的项目名称（8-25个中文字符）。
        - description 要包含项目整体价值 + 主要功能 + 推荐的主要组件类型（Button, Input, Table, Form, Card 等）。
        
        用户当前需求：%s
        """.formatted(retrievedContents, userPrompt);
    }

    /**
     * 从 Redis 向量库检索最相关的优秀项目案例
     * 直接利用已注入的 ragConfig.myRetriever()
     */
    private String retrieveExcellentCases(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return "暂无参考案例，请按通用最佳实践生成项目。";
        }
        try {
            // 获取 ContentRetriever
            dev.langchain4j.rag.content.retriever.ContentRetriever retriever =
                    ragConfig.myRetriever(embeddingStore, embeddingModel);
            // 构建检索请求
            Query query = Query.from(userPrompt);
            // 执行检索（直接传 Query，返回 List<Content>）
            List<Content> contents = retriever.retrieve(query);
            if (contents.isEmpty()) {
                return "暂无匹配的优秀案例，请按标准管理后台最佳实践生成。";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < contents.size(); i++) {
                sb.append("【优秀参考案例 ").append(i + 1).append("】\n")
                        .append(contents.get(i).textSegment().text())
                        .append("\n\n");
            }

            return sb.toString().trim();

        } catch (Exception e) {
            log.warn("RAG 检索优秀案例失败，使用默认提示", e);
            return "请参考平台上已有的优秀项目案例，按最佳实践生成项目结构和组件。";
        }
    }

    /**
     * 定时更新 AI 优秀项目案例（推荐每 3 小时执行一次）
     * 每次执行会把当前点赞量最高的 Top 5 项目重新向量化存入 Redis
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    @SchedulerLock(name = "createPerfectSampleLock", lockAtMostFor = "50m", lockAtLeastFor = "5m") // 防止多实例同时执行
    public void scheduledCreatePerfectSample() {
        long startTime = System.currentTimeMillis();
        log.info("========== 开始执行定时任务：更新 AI 优秀项目案例 ==========");

        try {
            createPerfectSample();

            long cost = System.currentTimeMillis() - startTime;
            log.info(" 定时任务执行完成：更新 AI 优秀项目案例成功，耗时 {}ms", cost);

        } catch (Exception e) {
            log.error(" 定时任务执行失败：更新 AI 优秀项目案例异常", e);
        }
    }

    /**
     * 构建 AI 所需优秀案例---向量化至redis，用于 RAG 时拿到优秀样例
     */
    public void createPerfectSample(){
        try {
            // 根据点赞量获取优秀项目
            ProjectRankDTO projectRankDTO = new ProjectRankDTO(false,true);
            List<ProjectRankVO> projectRank = projectService.getProjectRank(projectRankDTO);
            int size = Math.min(projectRank.size(), 5);
            if (size == 0) {
                log.warn("当前没有可作为优秀案例的项目，跳过本次任务");
                return;
            }
            log.info("本次将处理 {} 个优秀项目", size);

            // 先删除之前的所有向量记录
            clearExcellentSamples();

            for(int i=0;i<size;i++){
                // 先根据 projectId 得到对应项目的相关参数并赋值给 AiProjectSuggestion
                Long projectId = projectRank.get(i).getId();
                Project project = projectMapper.selectById(projectId);
                if (project == null) {
                    log.warn("项目不存在，跳过 -> projectId: {}", projectId);
                    continue;
                }

                // 构建此优秀项目 ai 回答模板
                AiProjectSuggestion aiProjectSuggestion = new AiProjectSuggestion(
                        projectId,
                        project.getProjectName(),
                        project.getDescription(),
                        project.getProjectType(),
                        project.getTechStack());

                // 创建元数据标记，用于后续精确删除
                Metadata metadata = Metadata.from(Map.of(
                        "projectType", "excellent_sample",
                        "projectId", projectId.toString(),
                        "projectName", project.getProjectName(),
                        "isPublic", "1",
                        "status", "SUCCESS"
                ));

                // 切块 + 向量化存入 Redis
                List<TextSegment> chunks = offLineManager.getAllChunks(aiProjectSuggestion, metadata);
                if (!chunks.isEmpty()) {
                    offLineManager.listEmbeddingsSave(chunks);
                    log.info("优秀案例已更新 -> 项目ID: {}, 名称: {}, 分块数量: {}",
                            projectId, project.getProjectName(), chunks.size());
                }
            }
        }
        catch (Exception e){
            log.error("createPerfectSample 执行失败", e);
        }
    }

    /**
     * 根据 projectId 删除该项目之前的所有向量记录（去重）
     */
    private void clearExcellentSamples() {

        try {
            log.info("执行全量清理优秀案例向量记录（保证去重）");
            embeddingStore.removeAll(metadataKey("projectType").isEqualTo("excellent_sample"));
            log.debug("已清空所有旧优秀案例向量");

        } catch (Exception e) {
            log.warn("清空向量记录时出现异常（不影响本次新增）", e);
        }
    }
}