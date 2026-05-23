package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageDSL {
    @JsonIgnore
    private Long id;
    private String route;               // 路由路径
    private String title;               // 页面标题
    private List<ComponentDSL> components;  // 当前页面所有组件
    private Integer currentVersion;           // 当前版本号（可选，方便前端展示）
    // 存储 AI 生成的原始代码
    private String PageGeneratedCode;
    // 用户修改后的最终代码（推荐）
    private String userEditedPageCode;
    @JsonIgnore
    private LocalDateTime createTime;
    @JsonIgnore
    private LocalDateTime updateTime;
}
