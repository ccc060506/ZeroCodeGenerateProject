package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ComponentDSL {

    @JsonIgnore
    private Long id;                        // 唯一标识
    private String type;                    // Button, Input, Table, Form, Card
    private String name;                    // 组件业务名称（表单字段名，建议和后端字段对应）
    private String label;                   // 显示标签（中文名）
    private Map<String, Object> props;      // 组件核心属性
    private Map<String, String> style;      // 样式配置
    private Object events;                  // 事件绑定
    private List<ComponentDSL> children;    // 子组件（支持递归嵌套）

    // ==================== 编辑态常用字段 ====================
    private Boolean visible = true;         // 是否显示
    private Integer sort;                   // 同级排序
    private String description;             // 组件描述

    // ==================== 辅助字段（推荐加上） ====================
    private String parentId;                // 父组件ID（便于移动、层级管理）
    private String pageId;                  // 所属页面ID（非常有用）

    /**
     * 无参构造时初始化常用集合，避免NPE
     */
    public ComponentDSL() {
        this.props = new HashMap<>();
        this.style = new HashMap<>();
        this.events = new HashMap<>();
        this.children = new ArrayList<>();
        this.visible = true;
    }
}
