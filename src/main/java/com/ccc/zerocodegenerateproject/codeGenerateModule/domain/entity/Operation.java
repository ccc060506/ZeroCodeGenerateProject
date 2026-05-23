package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Operation {

    private Long id;              // 操作ID
    private String type;            // update_props, update_style, add_component, delete_component, move_component, copy_component

    // 组件标识
    private String componentId;
    private String componentType;
    private String pageId;

    // 移动相关
    private String sourceParentId;
    private Integer sourcePosition;
    private String targetParentId;
    private Integer targetPosition;

    // 属性修改相关
    private Map<String, Object> oldProps;
    private Map<String, Object> newProps;
    private Map<String, String> oldStyle;
    private Map<String, String> newStyle;

    // 添加/复制相关
    private String newComponentId;
    private String parentId;
    private Integer position;
    private Map<String, Object> props;
    private Map<String, String> style;
}
