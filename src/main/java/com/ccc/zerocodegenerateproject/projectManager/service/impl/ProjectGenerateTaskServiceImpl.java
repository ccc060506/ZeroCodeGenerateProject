package com.ccc.zerocodegenerateproject.projectManager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.AiProjectSuggestion;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.ProjectGenerateTask;
import com.ccc.zerocodegenerateproject.projectManager.mapper.ProjectGenerateTaskMapper;
import com.ccc.zerocodegenerateproject.projectManager.service.ProjectGenerateTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectGenerateTaskServiceImpl extends ServiceImpl<ProjectGenerateTaskMapper, ProjectGenerateTask> implements ProjectGenerateTaskService
    {
        private final ProjectGenerateTaskMapper taskMapper;

        @Override
        public String createPendingTask(String prompt, Long userId) {
            String taskCode = "TASK_" + System.currentTimeMillis();

            ProjectGenerateTask task = ProjectGenerateTask.builder()
                    .taskCode(taskCode)
                    .userId(userId)
                    .genPrompt(prompt)
                    .status("PENDING")
                    .progress(0)
                    .errorMsg("无错误")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();

            taskMapper.insert(task);
            log.info("创建生成任务成功，taskCode: {}", taskCode);
            return taskCode;
        }

        @Override
        public void updateToRunning(String taskCode) {
            taskMapper.updateStatus(taskCode, "RUNNING", null, LocalDateTime.now());
        }

        @Override
        public Long completeGenerateTask(String taskCode, Long projectId , AiProjectSuggestion suggestion) {
            taskMapper.updateStatus(taskCode, "SUCCESS", null, LocalDateTime.now());
            log.info("任务完成，taskCode: {}", taskCode);
            return null;
        }

        @Override
        public void markAsFailed(String taskCode, String errorMsg) {
            taskMapper.updateStatus(taskCode, "FAILED", errorMsg, LocalDateTime.now());
            log.error("任务失败，taskCode: {}, error: {}", taskCode, errorMsg);
        }

        // 更新 AI 推断的结果刷新到 project_generate_task 表
        public void updateAiResult(String taskCode, AiProjectSuggestion aiProjectSuggestion){
            taskMapper.updateAiResult(taskCode,aiProjectSuggestion);
        }
}
