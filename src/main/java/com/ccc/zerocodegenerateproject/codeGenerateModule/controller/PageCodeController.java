package com.ccc.zerocodegenerateproject.codeGenerateModule.controller;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.StreamChunkMessage;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo.PageVO;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo.PageVersionVO;
import com.ccc.zerocodegenerateproject.codeGenerateModule.service.PageCodeService;
import com.ccc.zerocodegenerateproject.common.result.Result;
import com.ccc.zerocodegenerateproject.common.util.UserContext;
import com.ccc.zerocodegenerateproject.projectManager.service.ProjectService;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.UserLlmConfig;
import com.ccc.zerocodegenerateproject.userCenter.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import com.alibaba.fastjson.JSON;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PageCodeController {

    @Autowired
    private PageCodeService pageCodeService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private RedisMessageListenerContainer redisContainer;

    /**
     * 根据 项目Id 和 页面Id 和 版本号 versionNum 获取 页面信息用于展示
     * @param projectId
     * @param pageId
     * @param versionNum
     * @return
     */
    // 展示页面---ai 生成完之后的预览/查看页面详情
    @GetMapping("/page/project/{projectId}/page/{pageId}/versionNum/{versionNum}")
    public Result<PageVO> getPageInfo(
            @PathVariable("/projectId") long projectId,
            @PathVariable("/pageId") long pageId,
            @PathVariable("/versionNum") Integer versionNum
    ){
        log.info("查看项目id为{} , 页面id为 {} , 版本号为 {} 的页面详情",projectId,pageId,versionNum);
        PageVO pageVO = pageCodeService.getPageInfo(projectId,pageId,versionNum);
        return Result.success(pageVO);
    }

    /**
     * 新建项目内页面---ai 生成页面代码
     * @param projectId
     * @param pageId
     * @param prompt
     * @return
     */
    // 新建页面---ai 生成页面代码
    @PostMapping(value = "/page/generate-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> generateNewPageStream(
            @RequestParam Long projectId,
            @RequestParam Long pageId,
            @RequestBody String prompt) {
        log.info("创建项目id为{} , 页面id为 {} , 提示词为 {} 的页面 ",projectId,pageId,prompt);
        UserLlmConfig llmConfig = userMapper.selectUserLlmConfig(UserContext.getId());
        return pageCodeService.generateNewPageStream(projectId, pageId, prompt, llmConfig);
    }

    /**
     * 查看页面历史版本
     * @param projectId
     * @param pageId
     * @return
     */
    // 查看页面历史版本列表
    @GetMapping("/page/project/{projectId}/page/{pageId}")
    public Result<List<PageVersionVO>> getVersionPageInfo(
            @PathVariable("/projectId") long projectId,
            @PathVariable("/pageId") long pageId
    ){
        log.info("查看项目id为{} , 页面id为 {}  的页面 ",projectId,pageId);
        Boolean bool1 = pageCodeService.checkPageBelongsToProject(projectId, pageId);
        if(!bool1){
            return Result.error("此页面不属于该项目");
        }
        Boolean bool2 = projectService.checkProjectAccess(projectId);
        if (!bool2){
            return Result.error("您无访问权限");
        }
        List<PageVersionVO> res = pageCodeService.getVersionList(pageId);
        return Result.success(res);
    }

    /**
     * 删除指定版本的页面
     * @param projectId
     * @param pageId
     * @param version
     * @return
     */
    // 删除指定版本的页面
    @DeleteMapping("/page/project/{projectId}/page/{pageId}/{version}")
    public Result<String> deleteByVersion(
            @PathVariable("/projectId") long projectId,
            @PathVariable("/pageId") long pageId,
            @PathVariable("/version") Integer version
    ){
        log.info("删除项目id为{} , 页面id为 {} , 版本号为 {} 的页面 ",projectId,pageId,version);
        Boolean bool1 = pageCodeService.checkPageBelongsToProject(projectId, pageId);
        if(!bool1){
            return Result.error("此页面不属于该项目");
        }
        Boolean bool2 = projectService.checkProjectAccess(projectId);
        if (!bool2){
            return Result.error("您无访问权限");
        }
        Boolean bool3 = pageCodeService.deleteByVersion(pageId,version);
        if (bool3){
            return Result.success("删除成功");
        }
        return Result.error("删除失败");
    }

    // 从数据库直接查页面生成代码（前端 SSE 完成后刷新兜底）
    @GetMapping("/page/code")
    public Result<String> getPageCode(
            @RequestParam Long projectId,
            @RequestParam Long pageId) {
        log.info("查询页面代码 projectId={} pageId={}", projectId, pageId);
        String code = pageCodeService.getLatestPageCode(projectId, pageId);
        return Result.success(code);
    }

    // 从数据库直接查项目的完整 CodeDSL JSON
    @GetMapping("/project/{projectId}/dsl")
    public Result<String> getProjectDsl(@PathVariable Long projectId) {
        log.info("查询项目CodeDSL projectId={}", projectId);
        String dsl = pageCodeService.getProjectCodeDsl(projectId);
        return Result.success(dsl);
    }

    // 流式生成代码
    @GetMapping(value = "/sse/stream/{taskCode}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamCode(@PathVariable String taskCode) {
        // timeout 设为 0 = 永不超时（由业务逻辑控制关闭）
        SseEmitter emitter = new SseEmitter(0L);
        String channel = "stream:" + taskCode;
        MessageListenerAdapter[] listenerHolder = new MessageListenerAdapter[1];

        MessageListenerAdapter listener = new MessageListenerAdapter(
                (MessageListener) (message, pattern) -> {
                    try {
                        String raw = new String(message.getBody(), StandardCharsets.UTF_8);
                        String json = raw.startsWith("\"") ? JSON.parseObject(raw, String.class) : raw;
                        StreamChunkMessage msg = JSON.parseObject(json, StreamChunkMessage.class);

                        if ("chunk".equals(msg.getType())) {
                            emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(msg.getData()));

                        } else if ("done".equals(msg.getType())) {
                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data("[DONE]"));
                            emitter.complete();
                            redisContainer.removeMessageListener(listenerHolder[0]);

                        } else if ("error".equals(msg.getType())) {
                            emitter.send(SseEmitter.event()
                                    .name("streamError")
                                    .data(msg.getData()));
                            emitter.complete();
                            redisContainer.removeMessageListener(listenerHolder[0]);
                        }
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("ClientAbortException")) {
                            log.info("客户端已断开连接，停止推送");
                        } else {
                            log.error("SSE 推送失败", e);
                            emitter.completeWithError(e);
                        }
                    }
                }
        );
        listenerHolder[0] = listener;

        // 订阅 Redis 频道
        redisContainer.addMessageListener(listener, new PatternTopic(channel));

        // 注册监听器后，立刻发一个心跳
        try {
            emitter.send(SseEmitter.event()
                    .name("heartbeat")
                    .data("connected"));
        } catch (Exception e) {
            log.warn("心跳发送失败", e);
        }

        // 客户端断开时取消订阅
        emitter.onCompletion(() -> redisContainer.removeMessageListener(listener));
        emitter.onTimeout(() -> redisContainer.removeMessageListener(listener));

        return emitter;
    }
}
