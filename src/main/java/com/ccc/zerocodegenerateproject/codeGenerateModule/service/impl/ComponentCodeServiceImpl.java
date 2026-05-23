package com.ccc.zerocodegenerateproject.codeGenerateModule.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.CodeDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.ComponentDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.PageDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo.ComponentVO;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo.PageVO;
import com.ccc.zerocodegenerateproject.codeGenerateModule.mapper.ComponentCodeMapper;
import com.ccc.zerocodegenerateproject.codeGenerateModule.service.ComponentCodeService;
import com.ccc.zerocodegenerateproject.common.exception.BusinessException;
import com.ccc.zerocodegenerateproject.projectManager.domain.entity.Project;
import com.ccc.zerocodegenerateproject.projectManager.mapper.ProjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ComponentCodeServiceImpl extends ServiceImpl<ComponentCodeMapper, CodeDSL> implements ComponentCodeService {

    @Autowired
    private ProjectMapper projectMapper;
    @Autowired
    private DSLEngine dslEngine;

    // 查看组件详情
    public ComponentVO getComponentDetail(long projectId , long componentId){
        // 数据库中查找项目
        Project project = projectMapper.selectById(projectId);
        if(project == null){
            throw new BusinessException(404,"项目不存在或DSL为空");
        }

        // 解析对应项目的 DSL 用于查找组件
        CodeDSL codeDSL = dslEngine.parse(project.getCodeDSLJson());
        boolean exists = dslEngine.componentExists(codeDSL, componentId);
        if(!exists){
            throw new BusinessException("未找到组件，ID: " + componentId);
        }

        // 构建组件 VO 对象
        ComponentDSL component = dslEngine.findComponent(codeDSL, componentId);
        ComponentVO componentVO = new ComponentVO();
        BeanUtils.copyProperties(component,componentVO);
        PageDSL page = dslEngine.findPageByComponentId(codeDSL, componentId);
        componentVO.setProjectId(projectId);
        componentVO.setPageId(page.getId()==null?null:page.getId());
        componentVO.setPageTitle(page.getTitle()==null?null:page.getTitle());
        return componentVO;
    }
}
