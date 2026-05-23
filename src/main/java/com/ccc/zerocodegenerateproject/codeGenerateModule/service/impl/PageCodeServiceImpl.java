package com.ccc.zerocodegenerateproject.codeGenerateModule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.ai.AiPageCode;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.CodeDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.GeneratePageResult;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.PageDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.PageVersion;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo.PageVO;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo.PageVersionVO;
import com.ccc.zerocodegenerateproject.codeGenerateModule.mapper.PageCodeMapper;
import com.ccc.zerocodegenerateproject.codeGenerateModule.mapper.PageVersionMapper;
import com.ccc.zerocodegenerateproject.codeGenerateModule.service.PageCodeService;
import com.ccc.zerocodegenerateproject.codeGenerateModule.service.ScreenshotService;
import com.ccc.zerocodegenerateproject.common.exception.BusinessException;
import com.ccc.zerocodegenerateproject.common.util.UserContext;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import com.ccc.zerocodegenerateproject.projectManager.mapper.ProjectMapper;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.User;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.UserLlmConfig;
import com.ccc.zerocodegenerateproject.userCenter.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PageCodeServiceImpl extends ServiceImpl<PageCodeMapper, Project> implements PageCodeService {

    @Autowired
    private PageCodeMapper pageCodeMapper;
    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private DSLEngine dslEngine;
    @Autowired
    private PageVersionMapper pageVersionMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserMapper userMapper;
    private final ScreenshotService screenshotService;

    // 从数据库直接查项目的完整 codeDSLJson
    public String getProjectCodeDsl(long projectId) {
        Project project = projectMapper.selectById(projectId);
        return project != null ? project.getCodeDSLJson() : "";
    }

    // 从数据库直接查页面生成代码（前端刷新兜底用）
    public String getLatestPageCode(long projectId, long pageId) {
        try {
            PageVO vo = getPageInfo(projectId, pageId, 1);
            return vo.getPageGeneratedCode() != null ? vo.getPageGeneratedCode() : "";
        } catch (Exception e) {
            log.warn("getLatestPageCode failed for projectId={} pageId={}: {}", projectId, pageId, e.getMessage());
            return "";
        }
    }

    // 查看页面信息
    public PageVO getPageInfo(long projectId, long pageId, Integer versionNum){
        // 数据库中查找项目
        Project project = projectMapper.selectById(projectId);
        if(project == null){
            throw new BusinessException(404,"项目不存在或DSL为空");
        }

        // 解析对应项目的 DSL 用于查找页面
        CodeDSL codeDSL = dslEngine.parse(project.getCodeDSLJson());
        PageDSL page = dslEngine.findPage(codeDSL, pageId, versionNum);
        if(page == null){
            throw new BusinessException(404,"不存在id为"+ pageId + "的页面");
        }

        PageVO pageVO = new PageVO();
        BeanUtils.copyProperties(page , pageVO);
        pageVO.setPageDSL(page);                                      // 结构化数据给编辑器
        pageVO.setPageGeneratedCode(page.getPageGeneratedCode());     // AI原始生成代码
        pageVO.setUserEditedPageCode(page.getUserEditedPageCode());   // 用户改过的代码
        pageVO.setVersion(page.getCurrentVersion());

        return pageVO;
    }

    // 新建页面
    public Flux<ServerSentEvent<Object>> generateNewPageStream(Long projectId, Long pageId, String prompt, UserLlmConfig userLlmConfig){
        // 数据库中查找项目
        Project project = projectMapper.selectById(projectId);
        if(project == null){
            return Flux.just(ServerSentEvent.<Object>builder()
                    .event("error")
                    .data(Map.of("message", "项目不存在", "success", false))
                    .build());
        }

        // 解析该项目 CodeDSL 用于后续添加新页面
        String dslJson = project.getCodeDSLJson();
        CodeDSL codeDSL;
        if (StringUtils.isBlank(dslJson)) {
            // 首次创建项目时，构造一个空的 CodeDSL 对象
            log.info("==== 开始构建一个新项目 ====");
            codeDSL = new CodeDSL();
            codeDSL.setProjectId(project.getId());
            codeDSL.setProjectName(project.getProjectName());
            codeDSL.setPages(new ArrayList<>());   // 空页面列表
            codeDSL.setRawGeneratedCode("");
            codeDSL.setRenderedCode("");
        } else {
            codeDSL = dslEngine.parse(dslJson);
        }
        List<PageDSL> pages = codeDSL.getPages();
        if (pages == null) {
            pages = new ArrayList<>();
            codeDSL.setPages(pages);
        }
        for(int i=0;i<pages.size();i++){
            if (!pages.isEmpty()) {
                boolean exists = pages.stream()
                        .anyMatch(p -> Objects.equals(p.getId(), pageId));
                if (!exists) {
                    throw new BusinessException(404, "不存在id为" + pageId + "的页面");
                }
            }
        }

        // 校验用户 apiKey 和 modelName ---用于后续构建模型
        String apiKey = userLlmConfig.getApiKey();
        String modelName = userLlmConfig.getModelName();
        String provider = userLlmConfig.getProvider();
        if (StringUtils.isBlank(apiKey) || StringUtils.isBlank(modelName)) {
            throw new BusinessException(400, "apiKey 和 modelName 不能为空");
        }
        // 构建输出模型
        AiPageCode dynamicAiPageCode = createDynamicAiPageCode(apiKey, modelName, provider);

        // 用于累积完整结果
        StringBuilder fullJsonBuffer = new StringBuilder();
        return dynamicAiPageCode.generatePageCodeStream(prompt, project.getProjectType(), project.getTechStack())
                // 实时累积数据
                .doOnNext(fullJsonBuffer::append)
                // 实时推送给前端
                .map(chunk -> ServerSentEvent.<Object>builder()
                        .event("chunk")
                        .data(chunk)
                        .build())
                // 流结束后处理保存
                .concatWith(Flux.defer(() -> {
                    try {
                        // 解析完整的 JSON
                        String fullJson = fullJsonBuffer.toString();
                        GeneratePageResult result = parseJsonToResult(fullJson);
                        // 保存到数据库
                        int updateCount = saveGeneratedPage(projectId, pageId, result, project, codeDSL);
                        String finalPageKey = String.format("project:page:id:%d:v:%d", pageId, 1);
                        redisTemplate.opsForValue().set(finalPageKey, dslEngine.stringify(result.dsl()), 7, TimeUnit.DAYS);
                        // 返回成功事件
                        if(updateCount>0){
                            String base64 = screenshotService.renderAndScreenshot(result);
                            project.setPreviewImage(base64);
                            // 更新数据库预览图
                            updatePreviewImage(projectId,base64);
                            return Flux.just(ServerSentEvent.<Object>builder()
                                    .event("complete")
                                    .data(Map.of(
                                            "message", "生成并保存成功",
                                            "pageId", pageId,
                                            "result", result,    // 将累积的完整字符串传回，供前端校准覆盖
                                            "preview", base64
                                    ))
                                    .build());
                        }
                        return Flux.error(new BusinessException(500 , "保存到数据库失败"));
                    } catch (Exception e) {
                        log.error("=== thenMany 内部异常: ", e);
                        return Flux.just(ServerSentEvent.<Object>builder()
                                .event("error")
                                .data("保存失败: " + e.getMessage())
                                .build());
                    }
                }))
                .onErrorResume(e -> Flux.just(ServerSentEvent.<Object>builder()
                        .event("error")
                        .data("生成失败: " + e.getMessage())
                        .build()));
    }


    // 查询页面历史版本列表
    public List<PageVersionVO> getVersionList(long pageId){
        List<PageVersion> pageVersionList = pageCodeMapper.getVersionList(pageId);
        List<PageVersionVO> res = new ArrayList<>();
        for(PageVersion pageVersion:pageVersionList){
            PageVersionVO pageVersionVO = new PageVersionVO();
            BeanUtils.copyProperties(pageVersion, pageVersionVO);
            res.add(pageVersionVO);
        }
        return res;
    }

    // 检查页面是否属于此项目---用于校验
    public Boolean checkPageBelongsToProject(long projectId , long pageId){
        Project project = projectMapper.selectById(projectId);
        if(project == null){
            throw new BusinessException(404,"项目不存在或DSL为空");
        }

        CodeDSL codeDSL = dslEngine.parse(project.getCodeDSLJson());
        List<PageDSL> pages = codeDSL.getPages();
        for(PageDSL page:pages){
            if(page.getId() == pageId){
                return true;
            }
        }
        return false;
    }

    // 删除页面的指定历史版本
    public Boolean deleteByVersion(long pageId, Integer version){
        Boolean res = pageCodeMapper.deleteByVersion(pageId, version);
        return res==true;
    }

    /**
     * 解析流式结果为 GeneratePageResult
     */
    private GeneratePageResult parseJsonToResult(String fullText) {
        // 清理 markdown
        String text = fullText
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

        if (!text.startsWith("{")) {
            int start = text.indexOf("{");
            if (start != -1) text = text.substring(start);
        }

        log.info("=== 开始解析，总长度: {}", text.length());

        try {
            // 找到 "code": 的位置
            // AI 输出格式是 {"dsl": {...}, "code": "...vue代码..."}
            // 我们要把 dsl 部分和 code 部分彻底分开

            String dslJson;
            String codeValue = "";

            // 定位 "code" 字段的 key 位置
            int codeKeyPos = text.lastIndexOf("\"code\"");
            if (codeKeyPos != -1) {
                // "code" 后面找第一个 "，这是 code 值的开始引号
                int colonPos = text.indexOf(":", codeKeyPos);
                int codeValueStart = text.indexOf("\"", colonPos) + 1;

                // code 值的结束：倒数第2个字符是 "，最后一个字符是 }
                // 格式固定是 ..."vue代码..."}
                // 所以结束引号就是倒数第1个 " 之前（最后是 "}）
                int codeValueEnd = text.lastIndexOf("\"");

                if (codeValueStart > 0 && codeValueEnd > codeValueStart) {
                    // 提取 code 内容（包含原始换行，不需要转义处理）
                    codeValue = text.substring(codeValueStart, codeValueEnd);

                    // 截取 dsl 部分：从开头到 "code" key 之前
                    // 找到 "code" 前面的逗号位置
                    String beforeCode = text.substring(0, codeKeyPos).trim();
                    // 去掉末尾的逗号
                    if (beforeCode.endsWith(",")) {
                        beforeCode = beforeCode.substring(0, beforeCode.length() - 1);
                    }
                    dslJson = beforeCode + "}";
                } else {
                    dslJson = text;
                }
            } else {
                dslJson = text;
            }

            log.info("=== DSL 长度: {}，code 长度: {}", dslJson.length(), codeValue.length());
            log.info("=== dslJson 前100: {}", dslJson.substring(0, Math.min(100, dslJson.length())));
            log.info("=== dslJson 后100: {}", dslJson.substring(Math.max(0, dslJson.length()-100)));

            // 只解析 dsl 部分，code 已经是原始字符串不需要解析
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(
                    com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true
            );

            JsonNode root = mapper.readTree(dslJson);
            CodeDSL dsl = mapper.treeToValue(root.path("dsl"), CodeDSL.class);

            if (dsl == null) {
                throw new BusinessException(500, "dsl 字段为空");
            }

            if (dsl.getPages() != null) {
                dsl.getPages().forEach(p -> {
                    p.setId(null);
                    if (p.getComponents() != null) {
                        p.getComponents().forEach(c -> c.setId(null));
                    }
                });
            }

            return new GeneratePageResult(dsl, codeValue);

        } catch (Exception e) {
            log.error("解析失败", e);
            throw new BusinessException(500, "AI 返回格式错误");
        }
    }

    /**
     * 保存生成的页面
     */
    private int saveGeneratedPage(long projectId, long pageId, GeneratePageResult result, Project project, CodeDSL codeDSL) {

        log.info("==== 开始保存页面，projectId: {}, pageId: {} ====", projectId, pageId);
        List<PageDSL> resultPages = result.dsl().getPages();
        log.info("==== result.dsl pages 数量: {} ====", resultPages == null ? "null" : resultPages.size());
        if (resultPages == null || resultPages.isEmpty()) {
            log.error("AI 返回的 dsl.pages 为空，无法保存");
            throw new BusinessException(500, "AI 生成的页面结构为空");
        }
        List<PageDSL> pages = codeDSL.getPages();
        if (pages == null) {
            pages = new java.util.ArrayList<>();
            codeDSL.setPages(pages);
        }
        PageDSL pageDSL = resultPages.get(0);
        pageDSL.setId(pageId);
        pageDSL.setPageGeneratedCode(result.pageGeneratedCode());
        pageDSL.setCurrentVersion(1);
        pageDSL.setCreateTime(LocalDateTime.now());
        pageDSL.setUpdateTime(LocalDateTime.now());
        pages.add(pageDSL);
        codeDSL.setPages(pages);
        String newDSLJson = dslEngine.stringify(codeDSL);

        // 同步创建 PageVersion 并存入数据库
        PageVersion pageVersion = new PageVersion()
                .setPageDSLId(pageId)
                .setVersionNum(1)
                .setVersionRemark("v1.0 - AI初始生成")
                .setDslJson(dslEngine.stringify(pageDSL))
                .setRemark("AI根据用户提示词自动生成页面")
                .setOperator(UserContext.getUsername())
                .setCreateTime(LocalDateTime.now())
                .setStatus("PUBLISHED")
                .setPageGeneratedCode(pageDSL.getPageGeneratedCode())
                .setUserEditedPageCode(pageDSL.getUserEditedPageCode());

        PageVersion existing = pageVersionMapper.selectOne(
                new LambdaQueryWrapper<PageVersion>()
                        .eq(PageVersion::getPageDSLId, pageId)
                        .eq(PageVersion::getVersionNum, 1)
        );

        if (existing != null) {
            // 已存在就更新
            existing.setDslJson(dslEngine.stringify(pageDSL));
            existing.setPageGeneratedCode(pageDSL.getPageGeneratedCode());
            pageVersionMapper.updateById(existing);
        } else {
            pageVersionMapper.insert(pageVersion);
        }

        // 更新 project
        return pageCodeMapper.update(null, new LambdaUpdateWrapper<Project>()
                .eq(Project::getId, projectId)
                .eq(Project::getVersion, project.getVersion())
                .set(Project::getCodeDSLJson, newDSLJson)
                .set(Project::getVersion, project.getVersion() + 1)  // 版本号+1
                .set(Project::getUpdateTime, LocalDateTime  .now())
        );
    }

    private void updatePreviewImage(Long projectId, String base64){
        projectMapper.update(new LambdaUpdateWrapper<Project>()
                .eq(Project::getId,projectId)
                .set(Project::getPreviewImage,base64));
    }

    private AiPageCode createDynamicAiPageCode(String apiKey, String modelName, String provider) {
        StreamingChatModel streamingChatModel;

        // 默认配置：maxTokens 设大以避免 JSON 截断
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.7)
                .timeout(java.time.Duration.ofSeconds(120))
                .maxTokens(32768)
                .logRequests(true)
                .logResponses(true);

        // 根据 provider 切换 baseUrl
        if ("openai".equalsIgnoreCase(provider) || provider == null) {
            builder.modelName("gpt-4o");
        }
        else if ("deepseek".equalsIgnoreCase(provider)) {
            builder.baseUrl("https://api.deepseek.com");
            builder.modelName("deepseek-chat");
        }
        else if ("kimi".equalsIgnoreCase(provider)) {
            builder.baseUrl("https://api.moonshot.cn/v1");
            builder.modelName("kimi-k2.5");
        }
        else if ("grok".equalsIgnoreCase(provider)) {
            builder.baseUrl("https://api.x.ai/v1");
            builder.modelName("grok-4.20");
        }
        else if ("doubao".equalsIgnoreCase(provider)) {
            builder.baseUrl("https://ark.cn-beijing.volces.com/api/v3");
            builder.modelName("doubao-seed-2-0-code");
        }
        else if ("gemini".equalsIgnoreCase(provider)) {
            builder.baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/");
            builder.modelName("gemini-2.5-pro");
        }
        else {
            throw new BusinessException(400, "不支持的模型提供商: " + provider);
        }

        streamingChatModel = builder.build();
        // 动态构建 AiService 代理对象
        return AiServices.builder(AiPageCode.class)
                .streamingChatModel(streamingChatModel)
                .build();
    }


}