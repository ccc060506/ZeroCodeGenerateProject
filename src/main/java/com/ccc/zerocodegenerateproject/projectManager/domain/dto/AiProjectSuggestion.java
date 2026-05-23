package com.ccc.zerocodegenerateproject.projectManager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiProjectSuggestion {

    /**
     * 项目 Id
     */
    private Long projectId;

    /**
     * 项目名称：简洁、专业、有商业吸引力，长度控制在 8-25 个中文字符
     */
    private String projectName;

    /**
     * 项目详细描述：用一段话描述这个项目的主要功能和价值
     */
    private String description;

    /**
     * 项目类型，必须严格从以下值中选择一个：
     * CRM、OA、管理后台、H5应用、内部工具、电商、其他
     */
    private String projectType;

    /**
     * 推荐技术栈，必须严格从以下值中选择一个：
     * react_ant、vue_element、uniapp、html5、其他
     */
    private String techStack;
}
