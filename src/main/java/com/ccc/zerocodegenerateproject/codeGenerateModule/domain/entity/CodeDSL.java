package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeDSL {

    @JsonIgnore
    private Long projectId;        // 唯一标识
    private String projectName;    // 项目名称
    private List<PageDSL> pages;   // 页面列表
    // 重要：存储 AI 生成的原始代码
    private String rawGeneratedCode;    // AI 原始生成的完整代码（备份）
    private String renderedCode;        // 最终渲染后的代码（用户可编辑版）

}
