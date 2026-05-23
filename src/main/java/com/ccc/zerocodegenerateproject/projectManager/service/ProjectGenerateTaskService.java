package com.ccc.zerocodegenerateproject.projectManager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.AiProjectSuggestion;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.ProjectGenerateTask;

public interface ProjectGenerateTaskService extends IService<ProjectGenerateTask> {

    String createPendingTask(String prompt, Long userId);

    void updateToRunning(String taskCode);

    /**
     * 完成任务（推荐增加 projectId 参数）
     */
    Long completeGenerateTask(String taskCode, Long projectId, AiProjectSuggestion suggestion);

    void markAsFailed(String taskCode, String errorMsg);

    void updateAiResult(String taskCode, AiProjectSuggestion aiProjectSuggestion);
}
