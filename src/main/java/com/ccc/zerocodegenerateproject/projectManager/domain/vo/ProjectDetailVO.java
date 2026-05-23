package com.ccc.zerocodegenerateproject.projectManager.domain.vo;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectDetailVO {

    // 项目唯一编码
    @TableField(value = "project_code")
    private String projectCode;

    // 项目名称
    private String projectName;

    // 项目描述
    private String description;

    // 原始AI提示词
    private String aiPrompt;

    // 项目类型
    private String projectType;

    // 创建人名称
    private String username;

    // 技术栈
    private String techStack;

    // 创建时间
    private LocalDateTime createTime;

    /**
     * 浏览量（总计）
     */
    @TableField(value = "view_count")
    private Long viewCount = 0L;

    /**
     * 点赞数（总计）
     */
    @TableField(value = "like_count")
    private Long likeCount = 0L;

    // 当前登录用户是否已点赞该项目
    private Boolean liked;
}
