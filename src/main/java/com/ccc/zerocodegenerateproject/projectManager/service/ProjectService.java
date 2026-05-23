package com.ccc.zerocodegenerateproject.projectManager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.ProjectDTO;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.ProjectRankDTO;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.SelectDTO;
import com.ccc.zerocodegenerateproject.projectManager.domain.dto.SelectScrollDTO;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.ProjectGenerateTask;
import com.ccc.zerocodegenerateproject.projectManager.domain.vo.ProjectDetailVO;
import com.ccc.zerocodegenerateproject.projectManager.domain.vo.ProjectRankVO;
import com.ccc.zerocodegenerateproject.projectManager.domain.vo.ProjectVO;

import java.util.List;

public interface ProjectService extends IService<Project> {

    String newCreate(ProjectDTO projectDTO);

    List<ProjectVO> selectAll(SelectScrollDTO scrollDTO);

    List<ProjectVO> selectByRequest(SelectDTO selectDTO);

    ProjectDetailVO selectById(Long id);

    Boolean deleteById(Long id);

    void resetName(Long id, String newName);

    Boolean checkProjectAccess(long projectId);

    List<ProjectVO> selectHomeAll();

    Long likeOrUnlike(Long projectId,Boolean like);

    Boolean isUserLiked(Long projectId);

    List<ProjectRankVO> getProjectRank(ProjectRankDTO projectRankDTO);

    ProjectGenerateTask getByTaskCode(String taskCode);
}
