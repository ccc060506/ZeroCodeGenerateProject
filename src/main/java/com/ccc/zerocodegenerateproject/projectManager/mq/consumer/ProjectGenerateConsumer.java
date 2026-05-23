package com.ccc.zerocodegenerateproject.projectManager.mq.consumer;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.StreamChunkMessage;
import com.ccc.zerocodegenerateproject.codeGenerateModule.service.PageCodeService;
import com.ccc.zerocodegenerateproject.common.mq.RabbitMQConfig;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.AiProjectSuggestion;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.ProjectGenerateMessage;
import com.ccc.zerocodegenerateproject.projectManager.mapper.ProjectMapper;
import com.ccc.zerocodegenerateproject.projectManager.service.ProjectGenerateTaskService;
import com.ccc.zerocodegenerateproject.projectManager.service.impl.AiServiceImpl;
import com.ccc.zerocodegenerateproject.common.util.ProjectCodeGenerator;
import com.ccc.zerocodegenerateproject.userCenter.domain.entity.UserLlmConfig;
import com.ccc.zerocodegenerateproject.userCenter.mapper.UserMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.alibaba.fastjson.JSON;


@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectGenerateConsumer {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserMapper userMapper;
    private final ProjectGenerateTaskService taskService;
    private final ProjectMapper projectMapper;
    private final AiServiceImpl aiService;                    // AI 服务
    private final ProjectCodeGenerator projectCodeGenerator;
    private final PageCodeService pageCodeService;

    /**
     * 项目生成异步消费者
     * 这里实现 AI 调用 + Project 构建 + 插入的所有逻辑
     */
    @RabbitListener(queues = RabbitMQConfig.PROJECT_GENERATE_QUEUE, containerFactory = "rabbitListenerContainerFactory")
    /*@RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = RabbitMQConfig.PROJECT_GENERATE_QUEUE),
            exchange = @Exchange(name = RabbitMQConfig.PROJECT_GENERATE_EXCHANGE, type = ExchangeTypes.DIRECT),
            key = {RabbitMQConfig.PROJECT_GENERATE_ROUTING_KEY}
    ))*/
    public void handleProjectGenerate(ProjectGenerateMessage message,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        // 获取模型配置
        // 先查缓存
        String llmCache = "user:llm:id" + message.getUserId();
        UserLlmConfig llmConfig = (UserLlmConfig) redisTemplate.opsForValue().get(llmCache);
        if(llmConfig == null){
            llmConfig = userMapper.selectUserLlmConfig(message.getUserId());
            if(llmConfig != null) { // 只在查到数据时才写缓存
                redisTemplate.opsForValue().set(llmCache, llmConfig, 7, TimeUnit.DAYS);
            }
        }
        // 若此时仍为空 → 标记失败并确认消息，不重试
        if (llmConfig == null) {
            redisTemplate.opsForValue().set(llmCache, -1, 10, TimeUnit.SECONDS);
            log.warn("用户 {} 未配置 LLM，taskCode: {} 标记为失败", message.getUserId(), message.getTaskCode());
            taskService.markAsFailed(message.getTaskCode(), "用户未配置 LLM 信息，请前往个人中心配置");
            try { channel.basicAck(deliveryTag, false); } catch (Exception ignored) {}
            return;
        }

        log.info("开始处理生成任务 taskCode: {}", message.getTaskCode());
        try {
            // 更新任务状态为 RUNNING
            taskService.updateToRunning(message.getTaskCode());
            // 调用 AI 根据提示词解析
            AiProjectSuggestion suggestion = aiService.parseProjectFromPrompt(message.getGenPrompt(),llmConfig);
            // 生成项目编码
            String projectCode = projectCodeGenerator.generate();
            // 构建 Project 实体并赋值
            Project project = Project.builder()
                    .projectCode(projectCode)
                    .projectName(suggestion.getProjectName() != null ? suggestion.getProjectName() : "AI项目-" + System.currentTimeMillis())
                    .description(suggestion.getDescription())
                    .genPrompt(message.getGenPrompt())
                    .projectType(suggestion.getProjectType())
                    .techStack(suggestion.getTechStack() != null ? suggestion.getTechStack() : "react_ant")
                    .userId(message.getUserId())
                    .isPublic(1)
                    .viewCount(0L)
                    .likeCount(0L)
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            // 将 project 插入数据库
            project.setStatus("SUCCESS");
            projectMapper.insert(project);
            Long projectId = project.getId();
            Project savedProject = projectMapper.selectById(projectId);
            if (savedProject != null) {
                project = savedProject;
            }
            String projectCacheKey = "project:id" + projectId;
            redisTemplate.opsForValue().set(projectCacheKey, project, 7, TimeUnit.DAYS);

            // 更新 project_generate_task 中的 AI 字段
            suggestion.setProjectId(projectId);
            taskService.updateAiResult(message.getTaskCode(),suggestion);

            log.info("项目基本信息生成成功，projectId: {}, taskCode: {}", project.getId(), message.getTaskCode());


            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<Throwable> errorRef = new AtomicReference<>();
            // 开始异步生成代码
            try {
                log.info("开始异步生成页面代码，projectId: {}", projectId);

                pageCodeService.generateNewPageStream(projectId, 1L, message.getGenPrompt(), llmConfig )
                        .doOnNext(event -> {
                            if ("chunk".equals(event.event())) {
                                StreamChunkMessage msg = new StreamChunkMessage(
                                        "chunk", (String) event.data(), message.getTaskCode()
                                );
                                redisTemplate.convertAndSend(
                                        "stream:" + message.getTaskCode(),
                                        JSON.toJSONString(msg)
                                );
                            } else if ("complete".equals(event.event())) {
                                // 保存成功，发 done 给前端
                                StreamChunkMessage doneMsg = new StreamChunkMessage(
                                        "done", null, message.getTaskCode()
                                );
                                redisTemplate.convertAndSend(
                                        "stream:" + message.getTaskCode(),
                                        JSON.toJSONString(doneMsg)
                                );
                                taskService.completeGenerateTask(message.getTaskCode(), projectId, suggestion);
                                latch.countDown();
                            } else if ("error".equals(event.event())) {
                                // 业务错误（解析失败、保存失败等）
                                StreamChunkMessage errMsg = new StreamChunkMessage(
                                        "error", event.data() != null ? event.data().toString() : "生成失败",
                                        message.getTaskCode()
                                );
                                redisTemplate.convertAndSend(
                                        "stream:" + message.getTaskCode(),
                                        JSON.toJSONString(errMsg)
                                );
                                taskService.markAsFailed(message.getTaskCode(), "生成失败");
                                latch.countDown();
                            }
                        })
                        .doOnComplete(() -> {
                            // 这里只做兜底：如果 latch 还没 countDown（异常情况）
                            // 正常情况 complete 事件里已经 countDown 了
                            if (latch.getCount() > 0) {
                                latch.countDown();
                            }
                        })
                        .doOnError(e -> {
                            StreamChunkMessage errMsg = new StreamChunkMessage(
                                    "error", e.getMessage(), message.getTaskCode()
                            );
                            redisTemplate.convertAndSend(
                                    "stream:" + message.getTaskCode(),
                                    JSON.toJSONString(errMsg)
                            );
                            taskService.markAsFailed(message.getTaskCode(), e.getMessage());
                            errorRef.set(e);
                            latch.countDown();
                        })
                        .subscribe();

            } catch (Exception e) {
                log.error("生成页面代码失败，但项目基本信息已保存", e);
            }

            latch.await(5, TimeUnit.MINUTES);
            // 手动 ACK 确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理生成任务失败，taskCode: {}", message.getTaskCode(), e);
            // 更新任务为 FAILED
            taskService.markAsFailed(message.getTaskCode(), e.getMessage() != null ? e.getMessage() : "AI生成异常");
            try {
                // 拒绝消息，不重新入队（进入死信队列）
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                log.error("Nack 消息失败", ex);
            }
        }
    }
}
