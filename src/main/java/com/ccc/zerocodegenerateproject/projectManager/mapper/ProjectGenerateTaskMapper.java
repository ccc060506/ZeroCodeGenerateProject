package com.ccc.zerocodegenerateproject.projectManager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.AiProjectSuggestion;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.ProjectGenerateTask;
import org.apache.ibatis.annotations.Mapper;
import java.time.LocalDateTime;

@Mapper
public interface ProjectGenerateTaskMapper extends BaseMapper<ProjectGenerateTask> {
    void updateStatus(String taskCode, String running, Object o, LocalDateTime now);

    void updateAiResult(String taskCode, AiProjectSuggestion aiProjectSuggestion);
}
