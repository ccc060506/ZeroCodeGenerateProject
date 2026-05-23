package com.ccc.zerocodegenerateproject.projectManager.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.ProjectGenerateTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    void updateNameById(Long id, String newName);

    void updateStats(Long projectId, Long viewCountFromRedis, Long likeCountFromRedis);

    ProjectGenerateTask getByTaskCode(String taskCode);
}
