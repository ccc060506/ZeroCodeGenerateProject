package com.ccc.zerocodegenerateproject.codeGenerateModule.domain.vo;

import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.ComponentDSL;
import com.ccc.zerocodegenerateproject.codeGenerateModule.domain.entity.PageDSL;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageVO {

    private String title;                     // 页面标题
    private String route;                     // 路由路径
    private List<ComponentDSL> components;    // 当前页面所有组件
    private PageDSL pageDSL;
    private Integer version;                  // 当前版本号（用于乐观锁）
    private String pageGeneratedCode;         // AI 生成的完整前端代码（用于预览）
    private String userEditedPageCode;        // 用户手动修改后的代码（优先使用）
    private Boolean isEditable = true;        // 是否可编辑状态
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
