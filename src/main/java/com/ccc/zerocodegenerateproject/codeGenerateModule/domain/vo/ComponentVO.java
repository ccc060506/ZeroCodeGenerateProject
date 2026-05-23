package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.ComponentDSL;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ComponentVO {

    private Long projectId;           // 项目ID
    private Long pageId;            // 所在页面ID（非常重要！）
    private String pageTitle;         // 所在页面标题（方便前端显示）

    /**
     * 完整的组件信息
     * 前端会根据这个对象渲染属性面板
     */
    private ComponentDSL component;

    private String parentId;          // 父组件ID（如果是子组件）
    private Integer depth;            // 组件所在层级深度（可选）
    private Boolean isContainer;      // 是否是容器组件（有children）
}
