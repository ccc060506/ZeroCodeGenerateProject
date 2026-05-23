package com.ccc.zerocodegenerateproject.codeGenerateModule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.dto.ModifyDSLDTO;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;

public interface ModifyCodeService extends IService<Project> {

    Boolean updateComponent(Long projectId ,Long pageId,Integer versionNum, ModifyDSLDTO json);

    Boolean saveProject(Long projectId);
}
